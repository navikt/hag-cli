CREATE TABLE IF NOT EXISTS prod_import
(
    forespoersel_id           uuid,
    vedtaksperiode_id         uuid,
    eksponert_forespoersel_id uuid,
    status                    varchar,
    imported                  varchar
);

CREATE TABLE IF NOT EXISTS dev_import
(
    forespoersel_id           uuid,
    vedtaksperiode_id         uuid,
    eksponert_forespoersel_id uuid,
    status                    varchar,
    imported                  varchar
);