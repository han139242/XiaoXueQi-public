package com.neuedu.ems.security.phone;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.neuedu.ems.entity.mdm.MdmCustomer;
import com.neuedu.ems.entity.mdm.MdmSupplier;
import com.neuedu.ems.entity.org.OrgEmployee;
import com.neuedu.ems.mapper.mdm.MdmCustomerMapper;
import com.neuedu.ems.mapper.mdm.MdmSupplierMapper;
import com.neuedu.ems.mapper.org.OrgEmployeeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 只在显式启用的无 Web 进程中运行的旧明文回填命令。
 * 该组件从不在普通服务启动时自动迁移共享库。
 */
@Component
@ConditionalOnProperty(name = "mes.phone-crypto.backfill.enabled", havingValue = "true")
public class PhoneCryptoBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PhoneCryptoBackfillRunner.class);
    private static final int BATCH_SIZE = 200;
    private static final String CONFIRMATION = "RUN_CONTROLLED_PHONE_BACKFILL";

    private final Environment environment;
    private final OrgEmployeeMapper employeeMapper;
    private final MdmCustomerMapper customerMapper;
    private final MdmSupplierMapper supplierMapper;
    private final SensitivePhoneCryptoService cryptoService;
    private final PhoneNormalizer phoneNormalizer;
    private final PhoneLookupHashService lookupHashService;

    public PhoneCryptoBackfillRunner(Environment environment,
                                     OrgEmployeeMapper employeeMapper,
                                     MdmCustomerMapper customerMapper,
                                     MdmSupplierMapper supplierMapper,
                                     SensitivePhoneCryptoService cryptoService,
                                     PhoneNormalizer phoneNormalizer,
                                     PhoneLookupHashService lookupHashService) {
        this.environment = environment;
        this.employeeMapper = employeeMapper;
        this.customerMapper = customerMapper;
        this.supplierMapper = supplierMapper;
        this.cryptoService = cryptoService;
        this.phoneNormalizer = phoneNormalizer;
        this.lookupHashService = lookupHashService;
    }

    @Override
    public void run(ApplicationArguments args) {
        requireControlledInvocation();
        int employees = backfillEmployees();
        int customers = backfillCustomers();
        int suppliers = backfillSuppliers();
        log.info("联系电话受控回填完成：employees={}, customers={}, suppliers={}", employees, customers, suppliers);
    }

    private int backfillEmployees() {
        int updated = 0;
        while (true) {
            List<OrgEmployee> rows = employeeMapper.selectList(this.<OrgEmployee>pendingPhoneQuery("phone", "phone_lookup_hash")
                    .last("LIMIT " + BATCH_SIZE));
            if (rows.isEmpty()) return updated;
            for (OrgEmployee row : rows) {
                String normalized = normalizeStored(row.getPhone(), SensitivePhoneField.EMPLOYEE);
                OrgEmployee update = new OrgEmployee();
                update.setEmployeeId(row.getEmployeeId());
                update.setPhone(cryptoService.encrypt(normalized, SensitivePhoneField.EMPLOYEE));
                update.setPhoneLookupHash(lookupHashService.hashNormalized(normalized));
                if (employeeMapper.updateById(update) == 0) {
                    throw new IllegalStateException("employee phone backfill update failed");
                }
                updated++;
            }
        }
    }

    private int backfillCustomers() {
        int updated = 0;
        while (true) {
            List<MdmCustomer> rows = customerMapper.selectList(this.<MdmCustomer>pendingPhoneQuery("contact_phone", "contact_phone_lookup_hash")
                    .last("LIMIT " + BATCH_SIZE));
            if (rows.isEmpty()) return updated;
            for (MdmCustomer row : rows) {
                String normalized = normalizeStored(row.getContactPhone(), SensitivePhoneField.CUSTOMER);
                MdmCustomer update = new MdmCustomer();
                update.setCustomerId(row.getCustomerId());
                update.setContactPhone(cryptoService.encrypt(normalized, SensitivePhoneField.CUSTOMER));
                update.setContactPhoneLookupHash(lookupHashService.hashNormalized(normalized));
                if (customerMapper.updateById(update) == 0) {
                    throw new IllegalStateException("customer phone backfill update failed");
                }
                updated++;
            }
        }
    }

    private int backfillSuppliers() {
        int updated = 0;
        while (true) {
            List<MdmSupplier> rows = supplierMapper.selectList(this.<MdmSupplier>pendingPhoneQuery("contact_phone", "contact_phone_lookup_hash")
                    .last("LIMIT " + BATCH_SIZE));
            if (rows.isEmpty()) return updated;
            for (MdmSupplier row : rows) {
                String normalized = normalizeStored(row.getContactPhone(), SensitivePhoneField.SUPPLIER);
                MdmSupplier update = new MdmSupplier();
                update.setSupplierId(row.getSupplierId());
                update.setContactPhone(cryptoService.encrypt(normalized, SensitivePhoneField.SUPPLIER));
                update.setContactPhoneLookupHash(lookupHashService.hashNormalized(normalized));
                if (supplierMapper.updateById(update) == 0) {
                    throw new IllegalStateException("supplier phone backfill update failed");
                }
                updated++;
            }
        }
    }

    private <T> QueryWrapper<T> pendingPhoneQuery(String phoneColumn, String hashColumn) {
        return new QueryWrapper<T>()
                .eq("deleted_flag", 0)
                .isNotNull(phoneColumn)
                .and(wrapper -> wrapper.notLikeRight(phoneColumn, "v1:").or().isNull(hashColumn));
    }

    private String normalizeStored(String storedValue, SensitivePhoneField field) {
        String plainValue = cryptoService.isEnvelope(storedValue)
                ? cryptoService.decrypt(storedValue, field)
                : storedValue;
        return phoneNormalizer.normalizeForStorage(plainValue);
    }

    private void requireControlledInvocation() {
        String webType = environment.getProperty("spring.main.web-application-type", "servlet");
        if (!"none".equalsIgnoreCase(webType)) {
            throw new IllegalStateException("telephone backfill requires spring.main.web-application-type=none");
        }
        String confirmation = environment.getProperty("PHONE_CRYPTO_BACKFILL_CONFIRM", "");
        if (!CONFIRMATION.equals(confirmation)) {
            throw new IllegalStateException("PHONE_CRYPTO_BACKFILL_CONFIRM is required for telephone backfill");
        }
    }
}
