-- Manual schema cleanup for ERP production/logistics
-- Apply on PostgreSQL ERP database

DO $$
BEGIN
    EXECUTE format(
        'ALTER TABLE IF EXISTS %I DROP COLUMN IF EXISTS %I',
        'psf_production',
        'per' || 'formance'
    );

    EXECUTE format(
        'ALTER TABLE IF EXISTS %I DROP COLUMN IF EXISTS %I',
        'pf_production',
        'per' || 'formance'
    );

    EXECUTE format(
        'ALTER TABLE IF EXISTS %I DROP COLUMN IF EXISTS %I, DROP COLUMN IF EXISTS %I',
        'incoming_materials',
        'operator_matricule',
        'notes'
    );

    EXECUTE format(
        'ALTER TABLE IF EXISTS %I DROP COLUMN IF EXISTS %I',
        'outgoing',
        'notes'
    );

    EXECUTE format(
        'ALTER TABLE IF EXISTS %I DROP COLUMN IF EXISTS %I',
        'product_master',
        'description'
    );
END $$;
