ALTER TABLE ai_hui_hua DROP CHECK ck_ai_hui_hua_rounds;
ALTER TABLE ai_hui_hua
    ADD CONSTRAINT ck_ai_hui_hua_rounds CHECK (lei_ji_lun_shu BETWEEN 0 AND 10);
