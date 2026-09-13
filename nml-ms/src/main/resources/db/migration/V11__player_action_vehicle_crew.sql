-- Équipage précédent d'un SET_VEHICLE_CREW : pilote unique + passagers en CSV d'IDs.
ALTER TABLE public.player_actions
    ADD COLUMN prev_pilot_id bigint,
    ADD COLUMN prev_passenger_ids character varying(2000);
