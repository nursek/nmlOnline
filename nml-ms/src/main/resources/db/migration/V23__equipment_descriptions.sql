CREATE TABLE public.equipment_descriptions (
    equipment_id bigint NOT NULL,
    race character varying(20) NOT NULL,
    description character varying(600) NOT NULL,
    CONSTRAINT equipment_descriptions_pkey PRIMARY KEY (equipment_id, race),
    CONSTRAINT equipment_descriptions_race_check CHECK (((race)::text = ANY ((ARRAY['ORKS'::character varying, 'NECRONS'::character varying])::text[]))),
    CONSTRAINT equipment_descriptions_equipment_fk FOREIGN KEY (equipment_id) REFERENCES public.equipment(id) ON DELETE CASCADE
);
