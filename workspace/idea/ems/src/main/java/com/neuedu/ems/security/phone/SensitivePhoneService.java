package com.neuedu.ems.security.phone;

import com.neuedu.ems.common.ResultCode;
import com.neuedu.ems.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 集中处理电话字段的写入、展示和精确检索，业务资源不直接操作明文或密文。
 */
@Component
public class SensitivePhoneService {

    private final SensitivePhoneCryptoService cryptoService;
    private final PhoneLookupHashService lookupHashService;
    private final PhoneNormalizer phoneNormalizer;
    private final PhoneMasker phoneMasker;
    private final SensitivePhoneAccessPolicy accessPolicy;

    public SensitivePhoneService(SensitivePhoneCryptoService cryptoService,
                                 PhoneLookupHashService lookupHashService,
                                 PhoneNormalizer phoneNormalizer,
                                 PhoneMasker phoneMasker,
                                 SensitivePhoneAccessPolicy accessPolicy) {
        this.cryptoService = cryptoService;
        this.lookupHashService = lookupHashService;
        this.phoneNormalizer = phoneNormalizer;
        this.phoneMasker = phoneMasker;
        this.accessPolicy = accessPolicy;
    }

    public PhoneWriteValue prepareCreate(SensitivePhoneField field, String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return PhoneWriteValue.unchanged();
        }
        accessPolicy.requireWrite(field);
        return encrypt(field, rawPhone);
    }

    public PhoneWriteValue prepareUpdate(SensitivePhoneField field, String rawPhone) {
        if (rawPhone == null || !accessPolicy.canWrite(field)) {
            return PhoneWriteValue.unchanged();
        }
        if (rawPhone.isBlank()) {
            return PhoneWriteValue.clearValue();
        }
        return encrypt(field, rawPhone);
    }

    public String display(SensitivePhoneField field, String storedValue) {
        String normalizedPhone = cryptoService.decrypt(storedValue, field);
        return accessPolicy.canViewFull(field) ? normalizedPhone : phoneMasker.mask(normalizedPhone);
    }

    public Optional<String> lookupHash(String keyword) {
        return lookupHashService.hashLookupKeyword(keyword);
    }

    private PhoneWriteValue encrypt(SensitivePhoneField field, String rawPhone) {
        if (cryptoService.isEnvelope(rawPhone)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "联系电话不能提交密文格式");
        }
        String normalizedPhone = phoneNormalizer.normalizeForStorage(rawPhone);
        return PhoneWriteValue.replace(
                cryptoService.encrypt(normalizedPhone, field),
                lookupHashService.hashNormalized(normalizedPhone));
    }
}
