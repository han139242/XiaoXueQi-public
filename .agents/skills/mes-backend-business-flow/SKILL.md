---
name: mes-backend-business-flow
description: Use when implementing MES business workflows with state transitions, transactions, inventory movement, production execution, quality inspection, traceability, approval, or multi-table operations in the Spring Boot backend.
---

# MES Backend Business Flow

## Scope

Use this for high-risk MES flows:

- production order, plan, work order, dispatch, task, report;
- material issue, inbound, outbound, return, stocktake, inventory adjustment;
- inspection order, inspection result, nonconforming handling;
- barcode, product lot, material usage traceability;
- equipment repair or status change.

## Required Reading

- `COLLABORATION.md`
- `羽毛球MES开发约束框架.md`
- API document for the flow
- related `schema.sql` tables
- existing service/mapper patterns

## Workflow

1. Identify the business command, allowed source states, target state, and affected tables.
2. Confirm master-detail relationships and required transaction boundary.
3. Define validation before mutation: existence, `deleted_flag`, status, quantity, duplicate business number.
4. Put business logic in Service/ServiceImpl, not Controller.
5. Add `@Transactional` when more than one table changes or when state and records must stay consistent.
6. For inventory changes, write both current balance and movement/history records.
7. For status changes, record operation/status logs when the table design supports it.
8. Return domain-level failure through existing `ResultData`/exception conventions.
9. Run relevant backend tests or at least Maven test.
10. Update progress and risks in `COLLABORATION.md`.

## Quality Bar

- State changes are explicit and readable.
- Quantity operations check available quantity before deduction.
- Repeated calls do not silently duplicate critical records.
- Transaction boundary covers every required mutation.
- Logs or trace records are not skipped for core production, inventory, quality, or traceability flows.

## Must Not

- Do not directly update inventory quantities without movement records.
- Do not store Chinese status values in business state fields.
- Do not mix purchase/material batch and production product lot concepts.
- Do not implement complex business flow as a single Controller method.
