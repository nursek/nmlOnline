ALTER TABLE public.players
    ADD COLUMN race character varying(20);

CREATE TABLE public.equipment_translations (
    equipment_id bigint NOT NULL,
    race character varying(20) NOT NULL,
    display_name character varying(255) NOT NULL,
    CONSTRAINT equipment_translations_pkey PRIMARY KEY (equipment_id, race),
    CONSTRAINT equipment_translations_race_check CHECK (((race)::text = ANY ((ARRAY['ORKS'::character varying, 'NECRONS'::character varying])::text[]))),
    CONSTRAINT equipment_translations_equipment_fk FOREIGN KEY (equipment_id) REFERENCES public.equipment(id) ON DELETE CASCADE
);
