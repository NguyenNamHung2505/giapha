--
-- PostgreSQL database dump
--

\restrict 1kxWCNxyItJqJEgCYtHhWw83mjwyhoE0qdfaPnBYlLEXICOzpHdc3CDwhBmHrHE

-- Dumped from database version 14.20
-- Dumped by pg_dump version 14.20

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: unaccent; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA public;


--
-- Name: EXTENSION unaccent; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION unaccent IS 'text search dictionary that removes accents';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: events; Type: TABLE; Schema: public; Owner: familytree
--

CREATE TABLE public.events (
    id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    description text,
    event_date date,
    place character varying(500),
    type character varying(50) NOT NULL,
    individual_id uuid NOT NULL
);


ALTER TABLE public.events OWNER TO familytree;

--
-- Name: family_trees; Type: TABLE; Schema: public; Owner: familytree
--

CREATE TABLE public.family_trees (
    id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    description text,
    name character varying(255) NOT NULL,
    updated_at timestamp without time zone,
    owner_id uuid NOT NULL,
    cloned_at timestamp without time zone,
    source_individual_id uuid,
    source_tree_id uuid,
    root_individual_id uuid,
    admin_id uuid
);


ALTER TABLE public.family_trees OWNER TO familytree;

--
-- Name: individual_clone_mappings; Type: TABLE; Schema: public; Owner: familytree
--

CREATE TABLE public.individual_clone_mappings (
    id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    is_root_individual boolean NOT NULL,
    cloned_individual_id uuid NOT NULL,
    cloned_tree_id uuid NOT NULL,
    source_individual_id uuid NOT NULL,
    source_tree_id uuid NOT NULL
);


ALTER TABLE public.individual_clone_mappings OWNER TO familytree;

--
-- Name: individuals; Type: TABLE; Schema: public; Owner: familytree
--

CREATE TABLE public.individuals (
    id uuid NOT NULL,
    biography text,
    birth_date date,
    birth_place character varying(500),
    created_at timestamp without time zone NOT NULL,
    death_date date,
    death_place character varying(500),
    gender character varying(20),
    given_name character varying(255),
    suffix character varying(50),
    surname character varying(255),
    updated_at timestamp without time zone,
    tree_id uuid NOT NULL,
    profile_picture_url character varying(1000),
    notes text,
    facebook_link character varying(500),
    phone_number character varying(20),
    middle_name character varying(255)
);


ALTER TABLE public.individuals OWNER TO familytree;

--
-- Name: media; Type: TABLE; Schema: public; Owner: familytree
--

CREATE TABLE public.media (
    id uuid NOT NULL,
    caption text,
    file_size bigint,
    filename character varying(500) NOT NULL,
    mime_type character varying(100),
    storage_path character varying(1000) NOT NULL,
    type character varying(20) NOT NULL,
    uploaded_at timestamp without time zone NOT NULL,
    individual_id uuid NOT NULL
);


ALTER TABLE public.media OWNER TO familytree;

--
-- Name: relationships; Type: TABLE; Schema: public; Owner: familytree
--

CREATE TABLE public.relationships (
    id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    end_date date,
    start_date date,
    type character varying(50) NOT NULL,
    individual1_id uuid NOT NULL,
    individual2_id uuid NOT NULL,
    tree_id uuid NOT NULL
);


ALTER TABLE public.relationships OWNER TO familytree;

--
-- Name: tree_admins; Type: TABLE; Schema: public; Owner: familytree
--

CREATE TABLE public.tree_admins (
    tree_id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.tree_admins OWNER TO familytree;

--
-- Name: tree_invitations; Type: TABLE; Schema: public; Owner: familytree
--

CREATE TABLE public.tree_invitations (
    id uuid NOT NULL,
    accepted_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL,
    expires_at timestamp without time zone NOT NULL,
    invitee_email character varying(255) NOT NULL,
    role character varying(20) NOT NULL,
    status character varying(20) NOT NULL,
    token character varying(64) NOT NULL,
    accepted_by_user_id uuid,
    inviter_id uuid NOT NULL,
    tree_id uuid NOT NULL
);


ALTER TABLE public.tree_invitations OWNER TO familytree;

--
-- Name: tree_permissions; Type: TABLE; Schema: public; Owner: familytree
--

CREATE TABLE public.tree_permissions (
    id uuid NOT NULL,
    granted_at timestamp without time zone NOT NULL,
    role character varying(20) NOT NULL,
    tree_id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.tree_permissions OWNER TO familytree;

--
-- Name: user_tree_profiles; Type: TABLE; Schema: public; Owner: familytree
--

CREATE TABLE public.user_tree_profiles (
    id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    individual_id uuid NOT NULL,
    tree_id uuid NOT NULL,
    user_id uuid NOT NULL
);


ALTER TABLE public.user_tree_profiles OWNER TO familytree;

--
-- Name: users; Type: TABLE; Schema: public; Owner: familytree
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    created_at timestamp without time zone NOT NULL,
    email character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    password_hash character varying(255) NOT NULL,
    updated_at timestamp without time zone,
    admin boolean DEFAULT false,
    enabled boolean DEFAULT true,
    username character varying(50)
);


ALTER TABLE public.users OWNER TO familytree;

--
-- Data for Name: events; Type: TABLE DATA; Schema: public; Owner: familytree
--

COPY public.events (id, created_at, description, event_date, place, type, individual_id) FROM stdin;
\.


--
-- Data for Name: family_trees; Type: TABLE DATA; Schema: public; Owner: familytree
--

COPY public.family_trees (id, created_at, description, name, updated_at, owner_id, cloned_at, source_individual_id, source_tree_id, root_individual_id, admin_id) FROM stdin;
7912c254-9ea1-444d-a3e2-84b59623e310	2025-11-22 23:30:33.418269		NamHung	2025-11-22 23:30:33.418269	2ac7f34c-d6a4-44d9-846c-c2c70113d3bb	\N	\N	\N	\N	\N
eaeb7529-7a5b-430e-a1ef-81315dda9dfd	2025-11-29 12:55:09.630309	\N	Phạm Thị Hoa - Gia phả	2025-11-29 12:55:10.454372	b33c20d0-24be-4839-80a2-9de3d54d80c5	2025-11-29 12:55:09.629751	fbdbf5f0-d769-452a-ba26-f26422c90457	7912c254-9ea1-444d-a3e2-84b59623e310	cb8ced7b-0e12-4e4b-b1a8-e830da97ac17	\N
56b9bbbe-c713-4690-931f-79a70e8bddf7	2025-11-29 23:09:06.35542	\N	Phạm Thị Kim Cúc - Gia phả	2025-11-30 09:01:54.477213	b33c20d0-24be-4839-80a2-9de3d54d80c5	2025-11-29 23:09:06.34986	0501e72f-7c55-4b12-928b-329fbe5f7a8d	7912c254-9ea1-444d-a3e2-84b59623e310	0db9ce39-21a4-4961-b9c8-3cbf97ac0b59	6781a211-7601-4c56-ade1-1ff6d5e0239e
\.


--
-- Data for Name: individual_clone_mappings; Type: TABLE DATA; Schema: public; Owner: familytree
--

COPY public.individual_clone_mappings (id, created_at, is_root_individual, cloned_individual_id, cloned_tree_id, source_individual_id, source_tree_id) FROM stdin;
7ec4f301-46f5-4ef8-8801-968318fe6b91	2025-11-29 12:55:10.454897	f	15f504dd-f759-480e-bb22-29f30ae357d7	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	a721a1b3-e485-44b0-a768-845238cdadb2	7912c254-9ea1-444d-a3e2-84b59623e310
2745e832-2c4b-42bd-be88-4c91b52597ed	2025-11-29 12:55:10.457506	t	cb8ced7b-0e12-4e4b-b1a8-e830da97ac17	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	fbdbf5f0-d769-452a-ba26-f26422c90457	7912c254-9ea1-444d-a3e2-84b59623e310
9e8f5e22-eda3-45d5-8dbe-e75ea682bceb	2025-11-29 12:55:10.459114	f	f8bb80e4-5c95-41f6-82c8-d39a30960747	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	d8c6ce1c-befc-49da-a170-65c5ac04244b	7912c254-9ea1-444d-a3e2-84b59623e310
7bb9f3b5-263f-4b03-82b5-ad91b13c3430	2025-11-29 12:55:10.460701	f	b4b72d7f-66b1-454d-9a2d-d1aad3830f7a	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	0888fbcb-af0e-4bdc-977a-137e1e4aec19	7912c254-9ea1-444d-a3e2-84b59623e310
34d3cf4f-6459-418c-8e5c-d84e8af9e83e	2025-11-29 12:55:10.462271	f	633ba56e-0cb8-4a8e-b940-f9098213d76e	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	3192fe6b-8205-4e37-a7b5-e9f363cbd8e0	7912c254-9ea1-444d-a3e2-84b59623e310
8de62731-427c-42d9-ba84-cdc2d43b9739	2025-11-29 12:55:10.463846	f	6cc9a21a-5cc2-492b-8b69-b73c2c58d0ba	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	0501e72f-7c55-4b12-928b-329fbe5f7a8d	7912c254-9ea1-444d-a3e2-84b59623e310
d45ea3f0-6758-4719-9d4f-5c7bde4285d2	2025-11-29 23:09:07.485003	f	1dbb7940-4559-4fdf-bfaf-c9b5ac7c9f96	56b9bbbe-c713-4690-931f-79a70e8bddf7	0888fbcb-af0e-4bdc-977a-137e1e4aec19	7912c254-9ea1-444d-a3e2-84b59623e310
f88a142a-25eb-4063-8302-1e7be74afb86	2025-11-29 23:09:07.486003	f	c850cbd8-4e17-482b-a880-7698a3843d89	56b9bbbe-c713-4690-931f-79a70e8bddf7	3192fe6b-8205-4e37-a7b5-e9f363cbd8e0	7912c254-9ea1-444d-a3e2-84b59623e310
24364c4f-5ed5-49ab-b9d2-19d6aa965679	2025-11-29 23:09:07.490006	t	0db9ce39-21a4-4961-b9c8-3cbf97ac0b59	56b9bbbe-c713-4690-931f-79a70e8bddf7	0501e72f-7c55-4b12-928b-329fbe5f7a8d	7912c254-9ea1-444d-a3e2-84b59623e310
\.


--
-- Data for Name: individuals; Type: TABLE DATA; Schema: public; Owner: familytree
--

COPY public.individuals (id, biography, birth_date, birth_place, created_at, death_date, death_place, gender, given_name, suffix, surname, updated_at, tree_id, profile_picture_url, notes, facebook_link, phone_number, middle_name) FROM stdin;
cd87f497-ac19-4969-8ce5-23f9df76eef1		1931-01-01		2025-11-26 22:45:57.019396	2007-12-31		MALE		Hữu Cảnh	Nguyễn	2025-11-29 22:38:03.285179	7912c254-9ea1-444d-a3e2-84b59623e310	\N		\N	\N	\N
49d0fe2b-9784-4138-829a-32cfe73adefc		1929-01-01		2025-11-27 00:06:01.489956	2004-12-31		FEMALE	Cảnh		Bà	2025-11-29 22:38:16.020805	7912c254-9ea1-444d-a3e2-84b59623e310	\N		\N	\N	\N
a721a1b3-e485-44b0-a768-845238cdadb2		1963-07-24	Can Lộc, Hà Tĩnh	2025-11-23 21:59:44.876299	\N		MALE	Nguyên	Hữu	Nguyễn 	2025-11-29 22:38:33.900141	7912c254-9ea1-444d-a3e2-84b59623e310	/api/trees/7912c254-9ea1-444d-a3e2-84b59623e310/individuals/a721a1b3-e485-44b0-a768-845238cdadb2/avatar	\N	\N	\N	\N
0501e72f-7c55-4b12-928b-329fbe5f7a8d		1992-08-19	Vinh, Nghệ An	2025-11-23 22:04:13.904965	\N		FEMALE		Thị Kim Cúc	Phạm	2025-11-29 22:38:59.684959	7912c254-9ea1-444d-a3e2-84b59623e310	/api/trees/7912c254-9ea1-444d-a3e2-84b59623e310/individuals/0501e72f-7c55-4b12-928b-329fbe5f7a8d/avatar	\N	\N	\N	\N
0888fbcb-af0e-4bdc-977a-137e1e4aec19		2024-03-26	Vinh, Nghệ An	2025-11-23 22:11:39.416642	\N		FEMALE		Minh Ngọc	Nguyễn 	2025-11-24 22:37:13.163944	7912c254-9ea1-444d-a3e2-84b59623e310	/api/trees/7912c254-9ea1-444d-a3e2-84b59623e310/individuals/0888fbcb-af0e-4bdc-977a-137e1e4aec19/avatar	\N	\N	\N	\N
fbdbf5f0-d769-452a-ba26-f26422c90457		1964-05-31	Nam Đàn, Nghệ An	2025-11-24 23:06:26.756114	\N		FEMALE		Thị Hoa	Phạm	2025-11-29 22:39:13.13719	7912c254-9ea1-444d-a3e2-84b59623e310	/api/trees/7912c254-9ea1-444d-a3e2-84b59623e310/individuals/fbdbf5f0-d769-452a-ba26-f26422c90457/avatar	\N	\N	\N	\N
310b8467-6523-48c4-83aa-b8ab7765a68c		1949-12-31		2025-11-26 22:54:25.689865	\N		FEMALE		Thị Cảnh	Nguyễn	2025-11-26 22:54:25.689865	7912c254-9ea1-444d-a3e2-84b59623e310	\N		\N	\N	\N
8c1b33d1-6153-4125-992e-b6e87ae57f6e		1953-12-31		2025-11-26 22:54:57.762424	\N		FEMALE		Thị Huyên	Nguyễn	2025-11-26 22:54:57.762424	7912c254-9ea1-444d-a3e2-84b59623e310	\N		\N	\N	\N
2a7a5184-fa44-4e3a-b3f2-a24b586a3692		1957-12-31		2025-11-26 22:55:25.897431	\N		FEMALE		Thị Huyền	Nguyễn	2025-11-26 22:55:25.897431	7912c254-9ea1-444d-a3e2-84b59623e310	\N		\N	\N	\N
5b315776-aec4-47d5-8a37-ac639e87488c		1966-12-31		2025-11-26 22:56:03.520287	\N		FEMALE		Thị Nhân	Nguyễn	2025-11-26 22:56:03.520287	7912c254-9ea1-444d-a3e2-84b59623e310	\N		\N	\N	\N
1569eb89-ac28-47d2-87a9-e657d17deb85		1970-12-31		2025-11-26 22:56:29.495389	\N		FEMALE		Thị Lân	Nguyễn	2025-11-26 23:14:14.906362	7912c254-9ea1-444d-a3e2-84b59623e310	\N		\N	\N	\N
8eb59f9f-0622-4e8c-9c36-37411f2c4fbd		1992-01-22		2025-11-26 23:17:08.287875	\N		MALE		Duy Hải	Trần	2025-11-26 23:17:08.287875	7912c254-9ea1-444d-a3e2-84b59623e310	\N		\N	\N	\N
3192fe6b-8205-4e37-a7b5-e9f363cbd8e0	Đại học bách khoa Hà Nội	1992-05-25	Vinh , Nghệ An	2025-11-23 21:59:01.47218	\N		MALE	Hưng	Nam	Nguyễn	2025-11-29 22:36:52.544811	7912c254-9ea1-444d-a3e2-84b59623e310	/api/trees/7912c254-9ea1-444d-a3e2-84b59623e310/individuals/3192fe6b-8205-4e37-a7b5-e9f363cbd8e0/avatar	Trường THPT Phan Bội Châu	\N	\N	\N
d8c6ce1c-befc-49da-a170-65c5ac04244b		1998-03-06	Vinh, Nghệ An	2025-11-23 22:02:10.695604	\N		MALE		Kiên Tố	Nguyễn	2025-11-29 22:37:19.068875	7912c254-9ea1-444d-a3e2-84b59623e310	/api/trees/7912c254-9ea1-444d-a3e2-84b59623e310/individuals/d8c6ce1c-befc-49da-a170-65c5ac04244b/avatar	\N	\N	\N	\N
9b9d2235-6e88-417f-b2cb-f38c95f371f6		2014-01-01		2025-11-30 00:15:39.460335	\N		FEMALE	Phương	Minh	Hoàng	2025-11-30 00:15:39.460335	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N				\N
36f1f019-72b0-4465-80ce-6a9546d45f01		1975-01-01		2025-11-30 00:16:34.712584	\N		MALE	Tú	Khắc	Hoàng	2025-11-30 00:20:47.643769	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N				\N
121f2714-cabd-42d7-91f7-1924ea7cd62e		1935-01-01		2025-11-29 15:20:07.673015	1999-10-22		MALE	Thao	Quang	Phạm	2025-11-30 00:28:03.001848	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	\N				\N
c6b42cfa-1f48-47ed-ac72-4edeb9aa5894		1940-01-01		2025-11-30 00:09:39.407896	2019-04-15		FEMALE	Hường	Thị	Lê	2025-11-30 00:30:31.211283	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	\N				\N
08a9196d-9fca-4ab5-8e83-16184e1cbea8		1967-01-01		2025-11-30 00:52:50.619231	\N		MALE	Lý	Quang	Phạm	2025-11-30 00:52:50.619231	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	\N				\N
94e4216e-6de7-43d6-8e2b-53a40b256be2		2024-01-01		2025-11-30 09:25:21.616261	\N		MALE	Huy	Đức	Phạm	2025-11-30 09:25:21.616261	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N				\N
15f504dd-f759-480e-bb22-29f30ae357d7		1963-07-23	Can Lộc, Hà Tĩnh	2025-11-29 12:55:09.63944	\N		MALE	Nguyên	Hữu	Nguyễn 	2025-11-29 12:55:09.753017	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	/api/trees/eaeb7529-7a5b-430e-a1ef-81315dda9dfd/individuals/15f504dd-f759-480e-bb22-29f30ae357d7/avatar	\N	\N	\N	\N
cb8ced7b-0e12-4e4b-b1a8-e830da97ac17		1964-05-30	Nam Đàn, Nghệ An	2025-11-29 12:55:09.63944	\N		FEMALE		Thị Hoa	Phạm	2025-11-29 12:55:09.859421	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	/api/trees/eaeb7529-7a5b-430e-a1ef-81315dda9dfd/individuals/cb8ced7b-0e12-4e4b-b1a8-e830da97ac17/avatar	\N	\N	\N	\N
f8bb80e4-5c95-41f6-82c8-d39a30960747		1998-03-05	Vinh, Nghệ An	2025-11-29 12:55:09.638441	\N		MALE		Kiên Tố	Nguyễn	2025-11-29 12:55:09.972671	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	/api/trees/eaeb7529-7a5b-430e-a1ef-81315dda9dfd/individuals/f8bb80e4-5c95-41f6-82c8-d39a30960747/avatar	\N	\N	\N	\N
b4b72d7f-66b1-454d-9a2d-d1aad3830f7a		2024-03-26	Vinh, Nghệ An	2025-11-29 12:55:09.638441	\N		FEMALE		Minh Ngọc	Nguyễn 	2025-11-29 12:55:10.070762	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	/api/trees/eaeb7529-7a5b-430e-a1ef-81315dda9dfd/individuals/b4b72d7f-66b1-454d-9a2d-d1aad3830f7a/avatar	\N	\N	\N	\N
633ba56e-0cb8-4a8e-b940-f9098213d76e	Đại học bách khoa Hà Nội	1992-05-24	Vinh , Nghệ An	2025-11-29 12:55:09.63944	\N		MALE	Hưng	Nam	Nguyễn	2025-11-29 12:55:10.233388	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	/api/trees/eaeb7529-7a5b-430e-a1ef-81315dda9dfd/individuals/633ba56e-0cb8-4a8e-b940-f9098213d76e/avatar	Trường THPT Phan Bội Châu	\N	\N	\N
6cc9a21a-5cc2-492b-8b69-b73c2c58d0ba		1992-08-18	Vinh, Nghệ An	2025-11-29 12:55:09.638441	\N		FEMALE		Thị Kim Cúc	Phạm	2025-11-29 12:55:10.454897	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	/api/trees/eaeb7529-7a5b-430e-a1ef-81315dda9dfd/individuals/6cc9a21a-5cc2-492b-8b69-b73c2c58d0ba/avatar	\N	\N	\N	\N
c3539cea-cfd5-47ef-b769-78501fdfe8dd		1963-12-31		2025-11-29 23:09:06.386149	\N		MALE		Ngọc Hiệu	Phạm	2025-11-29 23:09:06.386149	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N	\N	\N	\N	\N
f7a99059-95c6-4a04-8ff2-9848d98a5f9d		1998-01-01		2025-11-29 23:09:06.387149	\N		MALE		Tiến Đạt	Phạm	2025-11-29 23:09:06.387149	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N		\N	\N	\N
8b763e7e-2bb8-417b-ba1e-0f718da79a5d		1965-01-01	Nghệ An	2025-11-29 23:09:06.38115	\N		FEMALE		Thị Loan	Nguyễn	2025-11-29 23:09:06.961254	56b9bbbe-c713-4690-931f-79a70e8bddf7	/api/trees/56b9bbbe-c713-4690-931f-79a70e8bddf7/individuals/8b763e7e-2bb8-417b-ba1e-0f718da79a5d/avatar	\N	\N	\N	\N
1dbb7940-4559-4fdf-bfaf-c9b5ac7c9f96		2024-03-26	Vinh, Nghệ An	2025-11-29 23:09:06.386149	\N		FEMALE		Minh Ngọc	Nguyễn 	2025-11-29 23:09:07.044558	56b9bbbe-c713-4690-931f-79a70e8bddf7	/api/trees/56b9bbbe-c713-4690-931f-79a70e8bddf7/individuals/1dbb7940-4559-4fdf-bfaf-c9b5ac7c9f96/avatar	\N	\N	\N	\N
c850cbd8-4e17-482b-a880-7698a3843d89	Đại học bách khoa Hà Nội	1992-05-25	Vinh , Nghệ An	2025-11-29 23:09:06.38867	\N		MALE	Hưng	Nam	Nguyễn	2025-11-29 23:09:07.248739	56b9bbbe-c713-4690-931f-79a70e8bddf7	/api/trees/56b9bbbe-c713-4690-931f-79a70e8bddf7/individuals/c850cbd8-4e17-482b-a880-7698a3843d89/avatar	Trường THPT Phan Bội Châu	\N	\N	\N
0db9ce39-21a4-4961-b9c8-3cbf97ac0b59		1992-08-19	Vinh, Nghệ An	2025-11-29 23:09:06.385149	\N		FEMALE		Thị Kim Cúc	Phạm	2025-11-29 23:09:07.46752	56b9bbbe-c713-4690-931f-79a70e8bddf7	/api/trees/56b9bbbe-c713-4690-931f-79a70e8bddf7/individuals/0db9ce39-21a4-4961-b9c8-3cbf97ac0b59/avatar	\N	\N	\N	\N
e2b8e295-d3c5-499a-85ee-cd8495404521		2012-01-01		2025-11-30 00:15:06.212314	\N		MALE	Thức	Minh	Hoàng	2025-11-30 00:15:06.212314	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N				\N
aac72157-29fd-4a4e-886b-7a3742d4e99b		2018-01-01		2025-11-30 00:16:10.079061	\N		FEMALE	Như	Quỳnh	Nguyễn	2025-11-30 00:16:10.079061	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N				\N
268eaf71-b06e-4f77-be92-e533a3583592		2023-01-01		2025-11-30 00:17:26.188297	\N		FEMALE	Khuê	Minh	Hoàng	2025-11-30 00:17:26.188297	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N				\N
18f265ed-52de-41d5-b81a-ca5dd0893b4d		2014-01-01		2025-11-30 00:18:44.336579	\N		MALE	Khiêm	Thanh	Nguyễn	2025-11-30 00:18:44.336579	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N				\N
67535c19-e69f-484c-9d68-fcb6dad9ed29		1987-01-01		2025-11-29 23:09:06.387149	\N		FEMALE		Thị Oanh	Phạm	2025-11-30 00:21:09.87517	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N		\N	\N	\N
d4852474-743d-44da-a533-7b9f03d2869d		1985-01-01		2025-11-29 23:09:06.387149	\N		FEMALE		 Thị Hương	Phạm	2025-11-30 00:21:28.664473	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N		\N	\N	\N
f65620b8-b84e-48ad-92c6-bb4e86cfdffd		1961-01-01		2025-11-30 00:38:13.832549	2025-01-01		MALE	Văn	Quang	Phạm	2025-11-30 00:38:13.832549	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	\N				\N
f9f082fd-bd5e-4a58-8c92-629d9cf3d13e		1990-01-01		2025-11-30 00:39:38.024353	\N		FEMALE	Thơ	Thị Anh	Phạm	2025-11-30 00:39:38.024353	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	\N				\N
58717850-7167-4523-ba82-79f100de0530		1984-01-01		2025-11-29 23:09:06.388148	\N		MALE		Thanh Sơn	Nguyễn 	2025-11-30 09:22:12.639359	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N		\N	\N	\N
263e4b6a-ae35-42d4-b7dc-8d5290e37686		1939-12-31		2025-11-29 23:09:06.386149	\N		MALE		Giao	Phạm	2025-11-30 09:23:47.139917	56b9bbbe-c713-4690-931f-79a70e8bddf7	/api/trees/56b9bbbe-c713-4690-931f-79a70e8bddf7/individuals/263e4b6a-ae35-42d4-b7dc-8d5290e37686/avatar		\N	\N	\N
1216fda7-bb8c-4020-a1b7-7abe292d31ff		1999-01-01		2025-11-30 09:24:37.57895	\N		FEMALE	Thuỷ	Thị	Nguyễn	2025-11-30 09:24:37.57895	56b9bbbe-c713-4690-931f-79a70e8bddf7	\N				\N
bc6daf58-95d1-4b10-b140-0848b3d2b1be		1993-01-01		2025-11-30 10:12:00.896341	\N		FEMALE	Thu	Thị Trang	Phạm	2025-11-30 10:12:00.896341	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	\N				\N
83045d0a-53a3-4ada-93d3-e6663806d943		1971-01-01		2025-11-30 10:12:29.174011	\N		MALE	Hoá	Quang	Phạm	2025-11-30 10:12:29.174011	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	\N				\N
37aa23af-0e0f-4974-8b42-db09340d6375		1975-01-01		2025-11-30 10:12:51.226734	\N		MALE	Khoa	Quang	Phạm	2025-11-30 10:12:51.226734	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	\N				\N
354d611b-5279-4ebd-8c69-545af1c1d94f		1966-01-01		2025-11-30 00:38:54.000405	\N		FEMALE	Thuý	Thị	Nguyễn	2025-11-30 23:14:11.893872	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	\N				\N
\.


--
-- Data for Name: media; Type: TABLE DATA; Schema: public; Owner: familytree
--

COPY public.media (id, caption, file_size, filename, mime_type, storage_path, type, uploaded_at, individual_id) FROM stdin;
3b13a8da-6e9f-4431-9017-50778d2dc2bd	\N	522669	IMG_0700.JPG	image/jpeg	7912c254-9ea1-444d-a3e2-84b59623e310/3192fe6b-8205-4e37-a7b5-e9f363cbd8e0/5e09ff11-9610-4aee-8447-efdc9d1eae21.JPG	PHOTO	2025-11-23 22:14:52.992232	3192fe6b-8205-4e37-a7b5-e9f363cbd8e0
cb0a2825-343d-493f-9cab-ed0f83f847b6	\N	522669	IMG_0700.JPG	image/jpeg	eaeb7529-7a5b-430e-a1ef-81315dda9dfd/633ba56e-0cb8-4a8e-b940-f9098213d76e/7c8323c3-a887-4b73-a04d-40b0037603ed.JPG	PHOTO	2025-11-29 12:55:10.175279	633ba56e-0cb8-4a8e-b940-f9098213d76e
7dd17aed-557d-4a94-bc79-3c0341638a17	\N	522669	IMG_0700.JPG	image/jpeg	56b9bbbe-c713-4690-931f-79a70e8bddf7/c850cbd8-4e17-482b-a880-7698a3843d89/987c587a-d11f-40cc-95e5-9e0c4b299e2e.JPG	PHOTO	2025-11-29 23:09:07.149729	c850cbd8-4e17-482b-a880-7698a3843d89
\.


--
-- Data for Name: relationships; Type: TABLE DATA; Schema: public; Owner: familytree
--

COPY public.relationships (id, created_at, end_date, start_date, type, individual1_id, individual2_id, tree_id) FROM stdin;
82615d4f-1b49-46b0-921e-b56bb9a4b871	2025-11-23 22:00:01.677939	\N	\N	PARENT_CHILD	a721a1b3-e485-44b0-a768-845238cdadb2	3192fe6b-8205-4e37-a7b5-e9f363cbd8e0	7912c254-9ea1-444d-a3e2-84b59623e310
bef44245-b623-4a17-87ba-3eaba2b38a37	2025-11-23 22:02:45.309054	\N	\N	SIBLING	d8c6ce1c-befc-49da-a170-65c5ac04244b	3192fe6b-8205-4e37-a7b5-e9f363cbd8e0	7912c254-9ea1-444d-a3e2-84b59623e310
a592d4e5-abb8-4553-98b4-68e81ae7d0c4	2025-11-23 22:03:34.578862	\N	\N	PARENT_CHILD	a721a1b3-e485-44b0-a768-845238cdadb2	d8c6ce1c-befc-49da-a170-65c5ac04244b	7912c254-9ea1-444d-a3e2-84b59623e310
9683eb5d-0079-44f2-a735-edac2a522363	2025-11-23 22:04:31.342153	\N	\N	SPOUSE	0501e72f-7c55-4b12-928b-329fbe5f7a8d	3192fe6b-8205-4e37-a7b5-e9f363cbd8e0	7912c254-9ea1-444d-a3e2-84b59623e310
c6769257-7425-4901-b3ed-e2eefd94e404	2025-11-23 22:11:56.703601	\N	\N	PARENT_CHILD	3192fe6b-8205-4e37-a7b5-e9f363cbd8e0	0888fbcb-af0e-4bdc-977a-137e1e4aec19	7912c254-9ea1-444d-a3e2-84b59623e310
302ca622-73ba-4652-bfc0-4e3d47c82eb7	2025-11-24 23:06:49.495044	\N	\N	SPOUSE	fbdbf5f0-d769-452a-ba26-f26422c90457	a721a1b3-e485-44b0-a768-845238cdadb2	7912c254-9ea1-444d-a3e2-84b59623e310
e2cdf38e-38ec-4ac8-b244-0040b70c4277	2025-11-24 23:54:41.14501	\N	\N	PARENT_CHILD	fbdbf5f0-d769-452a-ba26-f26422c90457	3192fe6b-8205-4e37-a7b5-e9f363cbd8e0	7912c254-9ea1-444d-a3e2-84b59623e310
0eefe68a-4faa-466d-a47c-877ac7347f3e	2025-11-24 23:54:47.129945	\N	\N	PARENT_CHILD	fbdbf5f0-d769-452a-ba26-f26422c90457	d8c6ce1c-befc-49da-a170-65c5ac04244b	7912c254-9ea1-444d-a3e2-84b59623e310
46bf4c84-0200-434b-82cb-c4c3976120a9	2025-11-26 00:24:37.398519	\N	\N	MOTHER_CHILD	0501e72f-7c55-4b12-928b-329fbe5f7a8d	0888fbcb-af0e-4bdc-977a-137e1e4aec19	7912c254-9ea1-444d-a3e2-84b59623e310
6c4da9d4-4165-4a00-861a-15c6c6a22eca	2025-11-26 23:00:02.446887	\N	\N	PARENT_CHILD	cd87f497-ac19-4969-8ce5-23f9df76eef1	a721a1b3-e485-44b0-a768-845238cdadb2	7912c254-9ea1-444d-a3e2-84b59623e310
4446dc07-d066-419b-b21c-7b9baf90eac6	2025-11-26 23:00:23.183755	\N	\N	PARENT_CHILD	cd87f497-ac19-4969-8ce5-23f9df76eef1	310b8467-6523-48c4-83aa-b8ab7765a68c	7912c254-9ea1-444d-a3e2-84b59623e310
dec51d8d-c378-453d-a267-d01fd1a9915a	2025-11-26 23:11:00.720206	\N	\N	FATHER_CHILD	cd87f497-ac19-4969-8ce5-23f9df76eef1	8c1b33d1-6153-4125-992e-b6e87ae57f6e	7912c254-9ea1-444d-a3e2-84b59623e310
3796e709-2dc0-4d5a-a0c7-50636f7f2168	2025-11-26 23:11:00.732965	\N	\N	SIBLING	8c1b33d1-6153-4125-992e-b6e87ae57f6e	a721a1b3-e485-44b0-a768-845238cdadb2	7912c254-9ea1-444d-a3e2-84b59623e310
fe0e5f21-4f1e-40c9-b1f1-a4504377a697	2025-11-26 23:11:00.737868	\N	\N	SIBLING	8c1b33d1-6153-4125-992e-b6e87ae57f6e	310b8467-6523-48c4-83aa-b8ab7765a68c	7912c254-9ea1-444d-a3e2-84b59623e310
2118616a-658a-45c8-a1f6-6ec15b3d774f	2025-11-26 23:11:10.361306	\N	\N	FATHER_CHILD	cd87f497-ac19-4969-8ce5-23f9df76eef1	2a7a5184-fa44-4e3a-b3f2-a24b586a3692	7912c254-9ea1-444d-a3e2-84b59623e310
53fa6998-5941-4c22-a5b1-6dedcd0bc1f8	2025-11-26 23:11:10.371667	\N	\N	SIBLING	2a7a5184-fa44-4e3a-b3f2-a24b586a3692	a721a1b3-e485-44b0-a768-845238cdadb2	7912c254-9ea1-444d-a3e2-84b59623e310
0b25d616-3abe-4a7b-aa8c-1ab8c5ee58bc	2025-11-26 23:11:10.377545	\N	\N	SIBLING	2a7a5184-fa44-4e3a-b3f2-a24b586a3692	310b8467-6523-48c4-83aa-b8ab7765a68c	7912c254-9ea1-444d-a3e2-84b59623e310
567480bd-5fc1-4790-bbb7-da1ed5a10afb	2025-11-26 23:11:10.38491	\N	\N	SIBLING	2a7a5184-fa44-4e3a-b3f2-a24b586a3692	8c1b33d1-6153-4125-992e-b6e87ae57f6e	7912c254-9ea1-444d-a3e2-84b59623e310
ac67de33-ddde-489f-bd79-4b5bfb3ca33d	2025-11-26 23:11:35.46939	\N	\N	FATHER_CHILD	cd87f497-ac19-4969-8ce5-23f9df76eef1	5b315776-aec4-47d5-8a37-ac639e87488c	7912c254-9ea1-444d-a3e2-84b59623e310
db8b5883-43b6-40d0-b54e-7eee1b5d3773	2025-11-26 23:11:35.47714	\N	\N	SIBLING	5b315776-aec4-47d5-8a37-ac639e87488c	a721a1b3-e485-44b0-a768-845238cdadb2	7912c254-9ea1-444d-a3e2-84b59623e310
662c72a5-c8fa-4f8a-8d8e-0ea9547b2cbe	2025-11-26 23:11:35.482782	\N	\N	SIBLING	5b315776-aec4-47d5-8a37-ac639e87488c	310b8467-6523-48c4-83aa-b8ab7765a68c	7912c254-9ea1-444d-a3e2-84b59623e310
eae24f6c-ebfc-48ab-a69c-1f536d43a7de	2025-11-26 23:11:35.486476	\N	\N	SIBLING	5b315776-aec4-47d5-8a37-ac639e87488c	8c1b33d1-6153-4125-992e-b6e87ae57f6e	7912c254-9ea1-444d-a3e2-84b59623e310
ece17184-b472-4a2e-8f86-51adc46ae241	2025-11-26 23:11:35.490203	\N	\N	SIBLING	5b315776-aec4-47d5-8a37-ac639e87488c	2a7a5184-fa44-4e3a-b3f2-a24b586a3692	7912c254-9ea1-444d-a3e2-84b59623e310
9a6e8352-e2c1-48c9-bf68-4713086b9cd0	2025-11-26 23:14:32.002319	\N	\N	FATHER_CHILD	cd87f497-ac19-4969-8ce5-23f9df76eef1	1569eb89-ac28-47d2-87a9-e657d17deb85	7912c254-9ea1-444d-a3e2-84b59623e310
f1a85bbc-7c1c-4a7e-812f-3157af4664be	2025-11-26 23:14:32.013425	\N	\N	SIBLING	1569eb89-ac28-47d2-87a9-e657d17deb85	a721a1b3-e485-44b0-a768-845238cdadb2	7912c254-9ea1-444d-a3e2-84b59623e310
7e20508f-2434-4e39-8f8c-a08f2b1c6a46	2025-11-26 23:14:32.01779	\N	\N	SIBLING	1569eb89-ac28-47d2-87a9-e657d17deb85	310b8467-6523-48c4-83aa-b8ab7765a68c	7912c254-9ea1-444d-a3e2-84b59623e310
5064b57e-af9c-4ee6-8039-b8e67f1e3bf2	2025-11-26 23:14:32.02349	\N	\N	SIBLING	1569eb89-ac28-47d2-87a9-e657d17deb85	8c1b33d1-6153-4125-992e-b6e87ae57f6e	7912c254-9ea1-444d-a3e2-84b59623e310
f0107dfc-931b-464b-9aad-8b296aeb37fd	2025-11-26 23:14:32.030351	\N	\N	SIBLING	1569eb89-ac28-47d2-87a9-e657d17deb85	2a7a5184-fa44-4e3a-b3f2-a24b586a3692	7912c254-9ea1-444d-a3e2-84b59623e310
6fc26a93-e8e3-4567-9823-ca6ca312ee5b	2025-11-26 23:14:32.034939	\N	\N	SIBLING	1569eb89-ac28-47d2-87a9-e657d17deb85	5b315776-aec4-47d5-8a37-ac639e87488c	7912c254-9ea1-444d-a3e2-84b59623e310
251eb365-eb0b-41dc-bd07-3866eee6def6	2025-11-26 23:17:24.697767	\N	\N	MOTHER_CHILD	1569eb89-ac28-47d2-87a9-e657d17deb85	8eb59f9f-0622-4e8c-9c36-37411f2c4fbd	7912c254-9ea1-444d-a3e2-84b59623e310
29b72340-461d-4e85-8bb2-964848532cac	2025-11-27 00:06:12.851517	\N	\N	SPOUSE	49d0fe2b-9784-4138-829a-32cfe73adefc	cd87f497-ac19-4969-8ce5-23f9df76eef1	7912c254-9ea1-444d-a3e2-84b59623e310
39398bdf-1fe3-4355-9a65-8129d4371b90	2025-11-27 00:07:31.741052	\N	\N	MOTHER_CHILD	49d0fe2b-9784-4138-829a-32cfe73adefc	a721a1b3-e485-44b0-a768-845238cdadb2	7912c254-9ea1-444d-a3e2-84b59623e310
029a1ce3-a399-4236-a15d-db725349501b	2025-11-27 00:29:12.952306	\N	\N	MOTHER_CHILD	49d0fe2b-9784-4138-829a-32cfe73adefc	8c1b33d1-6153-4125-992e-b6e87ae57f6e	7912c254-9ea1-444d-a3e2-84b59623e310
e6cf50d7-39c8-43da-873a-05b57ab5c624	2025-11-27 00:29:18.817695	\N	\N	MOTHER_CHILD	49d0fe2b-9784-4138-829a-32cfe73adefc	1569eb89-ac28-47d2-87a9-e657d17deb85	7912c254-9ea1-444d-a3e2-84b59623e310
c03b8a4b-ce0f-4c0f-bc1c-27da25a1b305	2025-11-27 00:29:24.850722	\N	\N	MOTHER_CHILD	49d0fe2b-9784-4138-829a-32cfe73adefc	310b8467-6523-48c4-83aa-b8ab7765a68c	7912c254-9ea1-444d-a3e2-84b59623e310
dfe55e88-827a-404a-8c6a-f1b62d1318e2	2025-11-27 00:29:24.861856	\N	\N	SIBLING	310b8467-6523-48c4-83aa-b8ab7765a68c	a721a1b3-e485-44b0-a768-845238cdadb2	7912c254-9ea1-444d-a3e2-84b59623e310
bd3ebe17-f2e8-428f-adee-8ec5d9670657	2025-11-27 00:29:35.744449	\N	\N	MOTHER_CHILD	49d0fe2b-9784-4138-829a-32cfe73adefc	5b315776-aec4-47d5-8a37-ac639e87488c	7912c254-9ea1-444d-a3e2-84b59623e310
d795b6ec-8890-48ff-8349-16c061a60eee	2025-11-29 12:55:09.64344	\N	\N	PARENT_CHILD	15f504dd-f759-480e-bb22-29f30ae357d7	633ba56e-0cb8-4a8e-b940-f9098213d76e	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
f62c6f44-9e92-4bf9-a5be-8618a66658ed	2025-11-29 12:55:09.644439	\N	\N	SIBLING	f8bb80e4-5c95-41f6-82c8-d39a30960747	633ba56e-0cb8-4a8e-b940-f9098213d76e	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
62619bb9-f0c3-4701-85ce-fb46d5ad9dfa	2025-11-29 12:55:09.644439	\N	\N	PARENT_CHILD	15f504dd-f759-480e-bb22-29f30ae357d7	f8bb80e4-5c95-41f6-82c8-d39a30960747	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
533afca2-70b9-4787-8f32-ae94d361a470	2025-11-29 12:55:09.644439	\N	\N	SPOUSE	6cc9a21a-5cc2-492b-8b69-b73c2c58d0ba	633ba56e-0cb8-4a8e-b940-f9098213d76e	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
e9c9c97e-44e8-43b1-8fec-7656b4e442cd	2025-11-29 12:55:09.644439	\N	\N	PARENT_CHILD	633ba56e-0cb8-4a8e-b940-f9098213d76e	b4b72d7f-66b1-454d-9a2d-d1aad3830f7a	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
842a9ab7-3d53-4af0-bdd8-13ce18877775	2025-11-29 12:55:09.644439	\N	\N	SPOUSE	cb8ced7b-0e12-4e4b-b1a8-e830da97ac17	15f504dd-f759-480e-bb22-29f30ae357d7	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
a2d04436-ed65-4770-a79b-cc2b083d499e	2025-11-29 12:55:09.645439	\N	\N	PARENT_CHILD	cb8ced7b-0e12-4e4b-b1a8-e830da97ac17	633ba56e-0cb8-4a8e-b940-f9098213d76e	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
73b29d33-3449-4c21-9d17-72f3b08f1723	2025-11-29 12:55:09.645439	\N	\N	PARENT_CHILD	cb8ced7b-0e12-4e4b-b1a8-e830da97ac17	f8bb80e4-5c95-41f6-82c8-d39a30960747	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
a11b0b0d-ad69-4d59-9a8d-5fc44e513e51	2025-11-29 12:55:09.646441	\N	\N	MOTHER_CHILD	6cc9a21a-5cc2-492b-8b69-b73c2c58d0ba	b4b72d7f-66b1-454d-9a2d-d1aad3830f7a	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
435dd5f8-6f93-4810-8063-d3047c0ea599	2025-11-29 15:20:15.456741	\N	\N	FATHER_CHILD	121f2714-cabd-42d7-91f7-1924ea7cd62e	cb8ced7b-0e12-4e4b-b1a8-e830da97ac17	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
83bf9cdc-412c-4e93-827c-b00cab895f97	2025-11-29 23:09:06.402345	\N	\N	SPOUSE	0db9ce39-21a4-4961-b9c8-3cbf97ac0b59	c850cbd8-4e17-482b-a880-7698a3843d89	56b9bbbe-c713-4690-931f-79a70e8bddf7
9226228f-63d0-47e5-a3c4-b0cbdbc130f8	2025-11-29 23:09:06.406052	\N	\N	PARENT_CHILD	c850cbd8-4e17-482b-a880-7698a3843d89	1dbb7940-4559-4fdf-bfaf-c9b5ac7c9f96	56b9bbbe-c713-4690-931f-79a70e8bddf7
386231dc-c69c-4032-a3b1-9d994097cc56	2025-11-29 23:09:06.406052	\N	\N	SPOUSE	c3539cea-cfd5-47ef-b769-78501fdfe8dd	8b763e7e-2bb8-417b-ba1e-0f718da79a5d	56b9bbbe-c713-4690-931f-79a70e8bddf7
5aea8ccd-b513-49ad-bcc8-8b15c49951b9	2025-11-29 23:09:06.406052	\N	\N	PARENT_CHILD	c3539cea-cfd5-47ef-b769-78501fdfe8dd	0db9ce39-21a4-4961-b9c8-3cbf97ac0b59	56b9bbbe-c713-4690-931f-79a70e8bddf7
58e2b8d0-dece-43d5-885c-d3ba66c0fe9b	2025-11-29 23:09:06.407054	\N	\N	MOTHER_CHILD	0db9ce39-21a4-4961-b9c8-3cbf97ac0b59	1dbb7940-4559-4fdf-bfaf-c9b5ac7c9f96	56b9bbbe-c713-4690-931f-79a70e8bddf7
7ba61e1c-cecd-4916-95fa-a5ebbfb30d22	2025-11-29 23:09:06.407054	\N	\N	MOTHER_CHILD	8b763e7e-2bb8-417b-ba1e-0f718da79a5d	0db9ce39-21a4-4961-b9c8-3cbf97ac0b59	56b9bbbe-c713-4690-931f-79a70e8bddf7
8be55609-7384-4ccf-9c2e-377868a18604	2025-11-29 23:09:06.407054	\N	\N	FATHER_CHILD	263e4b6a-ae35-42d4-b7dc-8d5290e37686	c3539cea-cfd5-47ef-b769-78501fdfe8dd	56b9bbbe-c713-4690-931f-79a70e8bddf7
41a94945-1f06-4f03-8dbb-d9edd125b41b	2025-11-29 23:09:06.407054	\N	\N	MOTHER_CHILD	8b763e7e-2bb8-417b-ba1e-0f718da79a5d	67535c19-e69f-484c-9d68-fcb6dad9ed29	56b9bbbe-c713-4690-931f-79a70e8bddf7
ec77ec5c-b23d-43b4-9fcd-448886930d0b	2025-11-29 23:09:06.407054	\N	\N	MOTHER_CHILD	8b763e7e-2bb8-417b-ba1e-0f718da79a5d	d4852474-743d-44da-a533-7b9f03d2869d	56b9bbbe-c713-4690-931f-79a70e8bddf7
0ca801aa-b2d4-4352-ae56-5002ead68bdd	2025-11-29 23:09:06.407054	\N	\N	FATHER_CHILD	c3539cea-cfd5-47ef-b769-78501fdfe8dd	67535c19-e69f-484c-9d68-fcb6dad9ed29	56b9bbbe-c713-4690-931f-79a70e8bddf7
d1192524-5364-4373-bc42-be3cd91855ca	2025-11-29 23:09:06.408053	\N	\N	FATHER_CHILD	c3539cea-cfd5-47ef-b769-78501fdfe8dd	d4852474-743d-44da-a533-7b9f03d2869d	56b9bbbe-c713-4690-931f-79a70e8bddf7
334ccbbe-13f3-4467-9395-98f40c4737fd	2025-11-29 23:09:06.408053	\N	\N	MOTHER_CHILD	8b763e7e-2bb8-417b-ba1e-0f718da79a5d	f7a99059-95c6-4a04-8ff2-9848d98a5f9d	56b9bbbe-c713-4690-931f-79a70e8bddf7
17519291-dbcf-4cb9-a621-4edee6d9ea6b	2025-11-29 23:09:06.408053	\N	\N	SIBLING	f7a99059-95c6-4a04-8ff2-9848d98a5f9d	0db9ce39-21a4-4961-b9c8-3cbf97ac0b59	56b9bbbe-c713-4690-931f-79a70e8bddf7
1090deed-bde2-44b2-a1f6-2ff1efb3f297	2025-11-29 23:09:06.408053	\N	\N	SIBLING	f7a99059-95c6-4a04-8ff2-9848d98a5f9d	67535c19-e69f-484c-9d68-fcb6dad9ed29	56b9bbbe-c713-4690-931f-79a70e8bddf7
8f1372e7-21fa-497b-af7f-65668cb9d572	2025-11-29 23:09:06.408053	\N	\N	SIBLING	f7a99059-95c6-4a04-8ff2-9848d98a5f9d	d4852474-743d-44da-a533-7b9f03d2869d	56b9bbbe-c713-4690-931f-79a70e8bddf7
c585112d-a66e-4329-9258-1b0019ccf381	2025-11-29 23:09:06.409053	\N	\N	FATHER_CHILD	c3539cea-cfd5-47ef-b769-78501fdfe8dd	f7a99059-95c6-4a04-8ff2-9848d98a5f9d	56b9bbbe-c713-4690-931f-79a70e8bddf7
4d8c3e40-3cf7-4aac-a0dc-08cfb84a23ba	2025-11-29 23:09:06.409053	\N	\N	SPOUSE	58717850-7167-4523-ba82-79f100de0530	67535c19-e69f-484c-9d68-fcb6dad9ed29	56b9bbbe-c713-4690-931f-79a70e8bddf7
ff9d5ca7-f2a4-49a5-9eb7-f67ede6e84b5	2025-11-30 00:09:52.705308	\N	\N	MOTHER_CHILD	c6b42cfa-1f48-47ed-ac72-4edeb9aa5894	cb8ced7b-0e12-4e4b-b1a8-e830da97ac17	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
a4e7f866-a0e5-4863-bcf6-cb9d1ca7d13b	2025-11-30 00:10:24.144853	\N	\N	SPOUSE	c6b42cfa-1f48-47ed-ac72-4edeb9aa5894	121f2714-cabd-42d7-91f7-1924ea7cd62e	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
69eab0ef-9d2c-4e3e-93a5-537fefb83c0d	2025-11-30 00:16:46.649694	\N	\N	SPOUSE	36f1f019-72b0-4465-80ce-6a9546d45f01	d4852474-743d-44da-a533-7b9f03d2869d	56b9bbbe-c713-4690-931f-79a70e8bddf7
a32da386-aa00-472e-9bdf-cba4be0f3244	2025-11-30 00:16:53.531358	\N	\N	FATHER_CHILD	36f1f019-72b0-4465-80ce-6a9546d45f01	e2b8e295-d3c5-499a-85ee-cd8495404521	56b9bbbe-c713-4690-931f-79a70e8bddf7
dbe8a536-ebff-4f73-854c-385d71aa2833	2025-11-30 00:16:59.981616	\N	\N	FATHER_CHILD	36f1f019-72b0-4465-80ce-6a9546d45f01	9b9d2235-6e88-417f-b2cb-f38c95f371f6	56b9bbbe-c713-4690-931f-79a70e8bddf7
f1b1b9c4-537f-403b-a870-151878820b01	2025-11-30 00:16:59.990933	\N	\N	SIBLING	9b9d2235-6e88-417f-b2cb-f38c95f371f6	e2b8e295-d3c5-499a-85ee-cd8495404521	56b9bbbe-c713-4690-931f-79a70e8bddf7
cdee3b94-adeb-4eec-9b1c-a4baae97750d	2025-11-30 00:17:40.332312	\N	\N	FATHER_CHILD	36f1f019-72b0-4465-80ce-6a9546d45f01	268eaf71-b06e-4f77-be92-e533a3583592	56b9bbbe-c713-4690-931f-79a70e8bddf7
3897695e-8f4b-47f9-a064-eda4a739b6c7	2025-11-30 00:17:40.339311	\N	\N	SIBLING	268eaf71-b06e-4f77-be92-e533a3583592	e2b8e295-d3c5-499a-85ee-cd8495404521	56b9bbbe-c713-4690-931f-79a70e8bddf7
aa0b25e1-747c-4586-8391-12aeb0b0cd51	2025-11-30 00:17:40.346311	\N	\N	SIBLING	268eaf71-b06e-4f77-be92-e533a3583592	9b9d2235-6e88-417f-b2cb-f38c95f371f6	56b9bbbe-c713-4690-931f-79a70e8bddf7
5272f898-1228-48ae-a8b4-5314c5046ab7	2025-11-30 00:18:01.455219	\N	\N	MOTHER_CHILD	d4852474-743d-44da-a533-7b9f03d2869d	9b9d2235-6e88-417f-b2cb-f38c95f371f6	56b9bbbe-c713-4690-931f-79a70e8bddf7
54cb0c74-5199-452f-9a0a-6053876d6fcb	2025-11-30 00:18:13.175864	\N	\N	MOTHER_CHILD	d4852474-743d-44da-a533-7b9f03d2869d	e2b8e295-d3c5-499a-85ee-cd8495404521	56b9bbbe-c713-4690-931f-79a70e8bddf7
2ddd6b6f-5979-4193-9dd1-9169d4e51bce	2025-11-30 00:18:17.384326	\N	\N	MOTHER_CHILD	d4852474-743d-44da-a533-7b9f03d2869d	268eaf71-b06e-4f77-be92-e533a3583592	56b9bbbe-c713-4690-931f-79a70e8bddf7
3527280b-9525-4fd9-af3b-03d62c021436	2025-11-30 00:19:13.775426	\N	\N	MOTHER_CHILD	67535c19-e69f-484c-9d68-fcb6dad9ed29	aac72157-29fd-4a4e-886b-7a3742d4e99b	56b9bbbe-c713-4690-931f-79a70e8bddf7
4005ee8e-2bd9-491d-9089-55656f51c029	2025-11-30 00:19:37.929798	\N	\N	MOTHER_CHILD	67535c19-e69f-484c-9d68-fcb6dad9ed29	18f265ed-52de-41d5-b81a-ca5dd0893b4d	56b9bbbe-c713-4690-931f-79a70e8bddf7
0ae8e757-1fb3-4d9f-92b3-ffbc3abc60df	2025-11-30 00:19:37.939784	\N	\N	SIBLING	18f265ed-52de-41d5-b81a-ca5dd0893b4d	aac72157-29fd-4a4e-886b-7a3742d4e99b	56b9bbbe-c713-4690-931f-79a70e8bddf7
886bb6e7-eb77-4cff-bb73-52ba47c12c85	2025-11-30 00:19:53.216417	\N	\N	FATHER_CHILD	58717850-7167-4523-ba82-79f100de0530	18f265ed-52de-41d5-b81a-ca5dd0893b4d	56b9bbbe-c713-4690-931f-79a70e8bddf7
d0ddeff3-6b4b-4d4e-a224-b811b90f2496	2025-11-30 00:20:15.890392	\N	\N	FATHER_CHILD	58717850-7167-4523-ba82-79f100de0530	aac72157-29fd-4a4e-886b-7a3742d4e99b	56b9bbbe-c713-4690-931f-79a70e8bddf7
e91c6ffb-cabd-414f-8de3-74ba60ec37e3	2025-11-30 09:25:39.884538	\N	\N	SPOUSE	1216fda7-bb8c-4020-a1b7-7abe292d31ff	f7a99059-95c6-4a04-8ff2-9848d98a5f9d	56b9bbbe-c713-4690-931f-79a70e8bddf7
93b21cfe-91ad-4eb5-ae0c-8134fde7f7a4	2025-11-30 09:25:53.46474	\N	\N	MOTHER_CHILD	1216fda7-bb8c-4020-a1b7-7abe292d31ff	94e4216e-6de7-43d6-8e2b-53a40b256be2	56b9bbbe-c713-4690-931f-79a70e8bddf7
f0119cf1-a225-494e-b63d-54639d9281a6	2025-11-30 09:25:53.498994	\N	\N	MOTHER_CHILD	f7a99059-95c6-4a04-8ff2-9848d98a5f9d	94e4216e-6de7-43d6-8e2b-53a40b256be2	56b9bbbe-c713-4690-931f-79a70e8bddf7
64f4021b-e732-4b09-9e10-39e9c4129adf	2025-11-30 09:36:20.547599	\N	\N	SPOUSE	354d611b-5279-4ebd-8c69-545af1c1d94f	f65620b8-b84e-48ad-92c6-bb4e86cfdffd	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
160e5c2d-d150-4a7b-aa2d-e125adfeebe2	2025-11-30 09:36:28.545331	\N	\N	MOTHER_CHILD	354d611b-5279-4ebd-8c69-545af1c1d94f	f9f082fd-bd5e-4a58-8c92-629d9cf3d13e	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
f9f1f321-c99d-4b19-8c4f-4363a16e0621	2025-11-30 09:36:28.579351	\N	\N	MOTHER_CHILD	f65620b8-b84e-48ad-92c6-bb4e86cfdffd	f9f082fd-bd5e-4a58-8c92-629d9cf3d13e	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
d3c22c5a-b25a-4ce8-9c52-c990c968957b	2025-11-30 09:37:19.424664	\N	\N	FATHER_CHILD	121f2714-cabd-42d7-91f7-1924ea7cd62e	08a9196d-9fca-4ab5-8e83-16184e1cbea8	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
896d0002-d19c-443f-99a6-3ded252ae2e3	2025-11-30 09:37:19.442231	\N	\N	SIBLING	08a9196d-9fca-4ab5-8e83-16184e1cbea8	cb8ced7b-0e12-4e4b-b1a8-e830da97ac17	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
32b1d812-6179-4d88-9673-84b0c2af0c3f	2025-11-30 09:37:19.46523	\N	\N	FATHER_CHILD	c6b42cfa-1f48-47ed-ac72-4edeb9aa5894	08a9196d-9fca-4ab5-8e83-16184e1cbea8	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
a55bbf02-cb30-4b3a-9140-1bee2f78b029	2025-11-30 09:37:25.8433	\N	\N	FATHER_CHILD	121f2714-cabd-42d7-91f7-1924ea7cd62e	f65620b8-b84e-48ad-92c6-bb4e86cfdffd	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
fc196de1-4363-47e3-81a4-f542e146a4e9	2025-11-30 09:37:25.860187	\N	\N	SIBLING	f65620b8-b84e-48ad-92c6-bb4e86cfdffd	08a9196d-9fca-4ab5-8e83-16184e1cbea8	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
b822b1b1-8935-409d-8a9d-34d0008d1fb4	2025-11-30 09:37:25.865945	\N	\N	SIBLING	f65620b8-b84e-48ad-92c6-bb4e86cfdffd	cb8ced7b-0e12-4e4b-b1a8-e830da97ac17	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
700e0d6b-cf75-4166-8306-8c8fdff85ec5	2025-11-30 09:37:25.887369	\N	\N	FATHER_CHILD	c6b42cfa-1f48-47ed-ac72-4edeb9aa5894	f65620b8-b84e-48ad-92c6-bb4e86cfdffd	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
c652691a-81d6-400e-931e-a2ef3db6b378	2025-11-30 12:30:40.985808	\N	\N	FATHER_CHILD	121f2714-cabd-42d7-91f7-1924ea7cd62e	37aa23af-0e0f-4974-8b42-db09340d6375	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
033e7bed-20b6-43f9-8089-8882c149d488	2025-11-30 12:30:40.999807	\N	\N	SIBLING	37aa23af-0e0f-4974-8b42-db09340d6375	08a9196d-9fca-4ab5-8e83-16184e1cbea8	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
1a453163-f32c-4774-adc2-4ceb9c369cf5	2025-11-30 12:30:41.005808	\N	\N	SIBLING	37aa23af-0e0f-4974-8b42-db09340d6375	cb8ced7b-0e12-4e4b-b1a8-e830da97ac17	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
ab295eee-d9fe-4d6c-a616-3d9bb9a33fec	2025-11-30 12:30:41.012633	\N	\N	SIBLING	37aa23af-0e0f-4974-8b42-db09340d6375	f65620b8-b84e-48ad-92c6-bb4e86cfdffd	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
40e5b82a-623f-4f54-95a8-e2771fd7d3fb	2025-11-30 12:30:41.028928	\N	\N	FATHER_CHILD	c6b42cfa-1f48-47ed-ac72-4edeb9aa5894	37aa23af-0e0f-4974-8b42-db09340d6375	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
ea7fcaaf-15dc-40d4-8a88-b2b7cfaa2b42	2025-11-30 12:30:49.117785	\N	\N	FATHER_CHILD	121f2714-cabd-42d7-91f7-1924ea7cd62e	83045d0a-53a3-4ada-93d3-e6663806d943	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
0390e2af-ca57-4a5f-9f65-abbdb63994d6	2025-11-30 12:30:49.131783	\N	\N	SIBLING	83045d0a-53a3-4ada-93d3-e6663806d943	08a9196d-9fca-4ab5-8e83-16184e1cbea8	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
1e610056-eb1d-46ef-b433-5bda45370052	2025-11-30 12:30:49.135782	\N	\N	SIBLING	83045d0a-53a3-4ada-93d3-e6663806d943	cb8ced7b-0e12-4e4b-b1a8-e830da97ac17	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
bb871a2c-6462-4b25-a011-e9258eadc780	2025-11-30 12:30:49.144068	\N	\N	SIBLING	83045d0a-53a3-4ada-93d3-e6663806d943	f65620b8-b84e-48ad-92c6-bb4e86cfdffd	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
ca9f1ab3-86db-440f-aea8-070a41d424c2	2025-11-30 12:30:49.148328	\N	\N	SIBLING	83045d0a-53a3-4ada-93d3-e6663806d943	37aa23af-0e0f-4974-8b42-db09340d6375	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
07d476eb-b7b3-41cb-9d5b-399ed2eeab28	2025-11-30 12:30:49.165856	\N	\N	FATHER_CHILD	c6b42cfa-1f48-47ed-ac72-4edeb9aa5894	83045d0a-53a3-4ada-93d3-e6663806d943	eaeb7529-7a5b-430e-a1ef-81315dda9dfd
\.


--
-- Data for Name: tree_admins; Type: TABLE DATA; Schema: public; Owner: familytree
--

COPY public.tree_admins (tree_id, user_id) FROM stdin;
56b9bbbe-c713-4690-931f-79a70e8bddf7	6781a211-7601-4c56-ade1-1ff6d5e0239e
56b9bbbe-c713-4690-931f-79a70e8bddf7	da7a84fc-ff5f-4693-ae0c-7781eb08e15a
56b9bbbe-c713-4690-931f-79a70e8bddf7	a6f99e91-348e-4355-9737-8d3c97c7652d
eaeb7529-7a5b-430e-a1ef-81315dda9dfd	f140a601-1616-4f08-a0b8-ee9d325eddf0
7912c254-9ea1-444d-a3e2-84b59623e310	0d9a29a2-b9fb-4043-8dd1-e99efd054a7d
7912c254-9ea1-444d-a3e2-84b59623e310	b33c20d0-24be-4839-80a2-9de3d54d80c5
7912c254-9ea1-444d-a3e2-84b59623e310	de445f9c-2fe4-44c9-8873-7954faa93286
\.


--
-- Data for Name: tree_invitations; Type: TABLE DATA; Schema: public; Owner: familytree
--

COPY public.tree_invitations (id, accepted_at, created_at, expires_at, invitee_email, role, status, token, accepted_by_user_id, inviter_id, tree_id) FROM stdin;
\.


--
-- Data for Name: tree_permissions; Type: TABLE DATA; Schema: public; Owner: familytree
--

COPY public.tree_permissions (id, granted_at, role, tree_id, user_id) FROM stdin;
\.


--
-- Data for Name: user_tree_profiles; Type: TABLE DATA; Schema: public; Owner: familytree
--

COPY public.user_tree_profiles (id, created_at, individual_id, tree_id, user_id) FROM stdin;
6e3b9ba4-7134-4d00-9168-ae0a082a2847	2025-11-28 00:31:10.498632	3192fe6b-8205-4e37-a7b5-e9f363cbd8e0	7912c254-9ea1-444d-a3e2-84b59623e310	b33c20d0-24be-4839-80a2-9de3d54d80c5
f40fcc98-15ca-4d01-a9b1-719b2d6a1fb4	2025-11-28 07:58:18.899492	a721a1b3-e485-44b0-a768-845238cdadb2	7912c254-9ea1-444d-a3e2-84b59623e310	0d9a29a2-b9fb-4043-8dd1-e99efd054a7d
578ab132-2dfd-4187-b910-84acdca030c5	2025-11-29 16:16:52.735745	d8c6ce1c-befc-49da-a170-65c5ac04244b	7912c254-9ea1-444d-a3e2-84b59623e310	de445f9c-2fe4-44c9-8873-7954faa93286
9701622d-d274-4bf3-8804-49ef8cf96477	2025-11-29 22:53:31.28888	cb8ced7b-0e12-4e4b-b1a8-e830da97ac17	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	f140a601-1616-4f08-a0b8-ee9d325eddf0
d4c296e3-5f38-4e79-aec7-17b94e58df9a	2025-11-29 22:56:22.738909	fbdbf5f0-d769-452a-ba26-f26422c90457	7912c254-9ea1-444d-a3e2-84b59623e310	f140a601-1616-4f08-a0b8-ee9d325eddf0
b0070d8f-9d67-4463-8d69-339a40a6da68	2025-11-29 23:08:09.195616	0501e72f-7c55-4b12-928b-329fbe5f7a8d	7912c254-9ea1-444d-a3e2-84b59623e310	6781a211-7601-4c56-ade1-1ff6d5e0239e
fac8eb42-8c32-4122-879d-b00962449f27	2025-11-29 23:08:34.922861	6cc9a21a-5cc2-492b-8b69-b73c2c58d0ba	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	6781a211-7601-4c56-ade1-1ff6d5e0239e
d1d996a4-0f56-41e8-b577-29d2fe417f88	2025-11-29 23:09:07.466479	c850cbd8-4e17-482b-a880-7698a3843d89	56b9bbbe-c713-4690-931f-79a70e8bddf7	b33c20d0-24be-4839-80a2-9de3d54d80c5
79c8a291-db34-46d4-a862-0d0b869fae37	2025-11-29 23:09:07.501123	0db9ce39-21a4-4961-b9c8-3cbf97ac0b59	56b9bbbe-c713-4690-931f-79a70e8bddf7	6781a211-7601-4c56-ade1-1ff6d5e0239e
ff027c07-687b-46d8-8e77-eb0a6ab95a5c	2025-11-29 23:14:45.835744	633ba56e-0cb8-4a8e-b940-f9098213d76e	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	b33c20d0-24be-4839-80a2-9de3d54d80c5
9107fa30-76ed-4d73-9d44-b5c7ad8b97c4	2025-11-29 23:14:53.659671	15f504dd-f759-480e-bb22-29f30ae357d7	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	0d9a29a2-b9fb-4043-8dd1-e99efd054a7d
15615a6e-56a1-465d-9f05-1d798517106a	2025-11-29 23:14:59.840493	f8bb80e4-5c95-41f6-82c8-d39a30960747	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	de445f9c-2fe4-44c9-8873-7954faa93286
3ecca7ee-5b7a-4bfb-8027-3afe8a4b65a1	2025-11-29 23:17:52.473507	121f2714-cabd-42d7-91f7-1924ea7cd62e	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	1d1df214-5b17-47e4-9a62-d080322a276b
b8f0a8f8-5c27-4e17-b69f-912045ddb93e	2025-11-29 23:17:52.591753	b4b72d7f-66b1-454d-9a2d-d1aad3830f7a	eaeb7529-7a5b-430e-a1ef-81315dda9dfd	2a91db3f-71f0-4778-ba3c-ffb83c3c169b
f540546f-3373-4a2e-ad7d-5499f7055159	2025-11-29 23:18:53.184537	0888fbcb-af0e-4bdc-977a-137e1e4aec19	7912c254-9ea1-444d-a3e2-84b59623e310	2a91db3f-71f0-4778-ba3c-ffb83c3c169b
2a5e6ec4-75bc-494f-bc42-4c1fcf437853	2025-11-29 23:20:51.581487	1dbb7940-4559-4fdf-bfaf-c9b5ac7c9f96	56b9bbbe-c713-4690-931f-79a70e8bddf7	2a91db3f-71f0-4778-ba3c-ffb83c3c169b
583c1d0d-ca85-4ddc-9e62-3cbaec0f71c7	2025-11-30 09:21:20.142467	67535c19-e69f-484c-9d68-fcb6dad9ed29	56b9bbbe-c713-4690-931f-79a70e8bddf7	da7a84fc-ff5f-4693-ae0c-7781eb08e15a
71bf18b4-0f5b-4bbb-b0ab-4a5847cbc075	2025-11-30 09:21:23.880644	d4852474-743d-44da-a533-7b9f03d2869d	56b9bbbe-c713-4690-931f-79a70e8bddf7	a6f99e91-348e-4355-9737-8d3c97c7652d
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: familytree
--

COPY public.users (id, created_at, email, name, password_hash, updated_at, admin, enabled, username) FROM stdin;
0d9a29a2-b9fb-4043-8dd1-e99efd054a7d	2025-11-28 07:57:54.340583	huunguyen@gmail.com	Nguyen Huu Nguyen	$2a$10$h7J3pPpcz0tgEITjOk9c2OeooJKCx8Z2NLmr9v57iFiVhrWrVaDaO	2025-11-28 07:57:54.340583	f	t	huunguyen
b33c20d0-24be-4839-80a2-9de3d54d80c5	2025-11-28 00:30:40.321715	hungnn22@viettel.com.vn	Nguyen Nam Hung	$2a$10$tFv/ycDLcjcm932XEhsXmeaUof5.OfnvlIU6iwOkUSlpeWtQWbWom	2025-11-29 09:20:30.894208	t	t	hungnn22
de445f9c-2fe4-44c9-8873-7954faa93286	2025-11-29 16:16:52.721137	nguyenkiento@familytree.local	Nguyễn Kiên Tố	$2a$10$7/4t0KGZL9wtVBoy.zZSle5RfBjRX/kJdpWWdXbhbGzrg4xWZkW52	2025-11-29 18:16:47.629008	f	t	nguyenkiento
f140a601-1616-4f08-a0b8-ee9d325eddf0	2025-11-29 22:53:31.260845	phamthihoa@familytree.local	Phạm Thị Hoa	$2a$10$l3w/uBbLGwwfLScKPAYqguFx8c5nejuuk8EeqEGXciDdfgGI0KoJq	2025-11-29 22:54:50.561126	f	t	phamthihoa
6781a211-7601-4c56-ade1-1ff6d5e0239e	2025-11-29 23:08:09.157983	phamthikimcuc@familytree.local	Phạm Thị Kim Cúc	$2a$10$8jYshH.kCIXW1rpBUt.oDu5mFPCUlUIvgGXlGrYraiyY0lul6zFz.	2025-11-29 23:08:09.157983	f	t	phamthikimcuc
1d1df214-5b17-47e4-9a62-d080322a276b	2025-11-29 23:17:52.472991	phamquangthao@familytree.local	Phạm Quang Thao	$2a$10$bJkraJbAUh7TDMW6TynQheFmFI55UZR4Pf7bWsb0ZjB1pNI0pwguq	2025-11-29 23:17:52.472991	f	t	phamquangthao
2a91db3f-71f0-4778-ba3c-ffb83c3c169b	2025-11-29 23:17:52.591753	nguyenminhngoc@familytree.local	Nguyễn  Minh Ngọc	$2a$10$Dn1bT3UXgJ7x9oUI9X/P.ur6jv1dd6eUna7RKciuKpkR0EJNa4x7u	2025-11-29 23:17:52.591753	f	t	nguyenminhngoc
da7a84fc-ff5f-4693-ae0c-7781eb08e15a	2025-11-30 09:21:20.116858	phamthioanh@familytree.local	Phạm Thị Oanh	$2a$10$ZNJsJu5TX.URuaOz9yXrTeN0RprF64x7ebgzFVyqy5WTguoD7aQ9K	2025-11-30 09:21:20.116858	f	t	phamthioanh
a6f99e91-348e-4355-9737-8d3c97c7652d	2025-11-30 09:21:23.880644	phamthihuong@familytree.local	Phạm  Thị Hương	$2a$10$gRRjApBrmZgiWKx1mTwYnOq6PX5u2MSqLQcUUE/iyyifol7B.aTpK	2025-11-30 09:21:23.880644	f	t	phamthihuong
2ac7f34c-d6a4-44d9-846c-c2c70113d3bb	2025-11-22 14:47:51.739998	test@example.com	Test User	$2a$10$n2Gin0.5o.M7UZsET0ZyY.9sNCZGRpKhcp9UMCm1NomO5zvP9RM.m	2025-11-30 10:10:12.615521	t	t	test
\.


--
-- Name: events events_pkey; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT events_pkey PRIMARY KEY (id);


--
-- Name: family_trees family_trees_pkey; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.family_trees
    ADD CONSTRAINT family_trees_pkey PRIMARY KEY (id);


--
-- Name: individual_clone_mappings individual_clone_mappings_pkey; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.individual_clone_mappings
    ADD CONSTRAINT individual_clone_mappings_pkey PRIMARY KEY (id);


--
-- Name: individuals individuals_pkey; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.individuals
    ADD CONSTRAINT individuals_pkey PRIMARY KEY (id);


--
-- Name: media media_pkey; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.media
    ADD CONSTRAINT media_pkey PRIMARY KEY (id);


--
-- Name: relationships relationships_pkey; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.relationships
    ADD CONSTRAINT relationships_pkey PRIMARY KEY (id);


--
-- Name: tree_admins tree_admins_pkey; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.tree_admins
    ADD CONSTRAINT tree_admins_pkey PRIMARY KEY (tree_id, user_id);


--
-- Name: tree_invitations tree_invitations_pkey; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.tree_invitations
    ADD CONSTRAINT tree_invitations_pkey PRIMARY KEY (id);


--
-- Name: tree_permissions tree_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.tree_permissions
    ADD CONSTRAINT tree_permissions_pkey PRIMARY KEY (id);


--
-- Name: users uk_6dotkott2kjsp8vw4d0m25fb7; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk_6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);


--
-- Name: tree_invitations uk_dgpwn6t1hg7lehtewyws14atv; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.tree_invitations
    ADD CONSTRAINT uk_dgpwn6t1hg7lehtewyws14atv UNIQUE (token);


--
-- Name: individual_clone_mappings uk_source_cloned_individual; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.individual_clone_mappings
    ADD CONSTRAINT uk_source_cloned_individual UNIQUE (source_individual_id, cloned_individual_id);


--
-- Name: tree_permissions uk_tree_user; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.tree_permissions
    ADD CONSTRAINT uk_tree_user UNIQUE (tree_id, user_id);


--
-- Name: user_tree_profiles ukjwwcyeysfh5lxdhyd8m208e22; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.user_tree_profiles
    ADD CONSTRAINT ukjwwcyeysfh5lxdhyd8m208e22 UNIQUE (user_id, tree_id);


--
-- Name: user_tree_profiles user_tree_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.user_tree_profiles
    ADD CONSTRAINT user_tree_profiles_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: idx_clone_mapping_cloned; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_clone_mapping_cloned ON public.individual_clone_mappings USING btree (cloned_individual_id);


--
-- Name: idx_clone_mapping_cloned_tree; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_clone_mapping_cloned_tree ON public.individual_clone_mappings USING btree (cloned_tree_id);


--
-- Name: idx_clone_mapping_source; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_clone_mapping_source ON public.individual_clone_mappings USING btree (source_individual_id);


--
-- Name: idx_clone_mapping_source_tree; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_clone_mapping_source_tree ON public.individual_clone_mappings USING btree (source_tree_id);


--
-- Name: idx_event_date; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_event_date ON public.events USING btree (event_date);


--
-- Name: idx_event_individual; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_event_individual ON public.events USING btree (individual_id);


--
-- Name: idx_event_type; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_event_type ON public.events USING btree (type);


--
-- Name: idx_individual_birth; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_individual_birth ON public.individuals USING btree (birth_date);


--
-- Name: idx_individual_death; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_individual_death ON public.individuals USING btree (death_date);


--
-- Name: idx_individual_name; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_individual_name ON public.individuals USING btree (given_name, surname);


--
-- Name: idx_individual_tree; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_individual_tree ON public.individuals USING btree (tree_id);


--
-- Name: idx_invitation_email; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_invitation_email ON public.tree_invitations USING btree (invitee_email);


--
-- Name: idx_invitation_token; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_invitation_token ON public.tree_invitations USING btree (token);


--
-- Name: idx_invitation_tree; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_invitation_tree ON public.tree_invitations USING btree (tree_id);


--
-- Name: idx_media_individual; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_media_individual ON public.media USING btree (individual_id);


--
-- Name: idx_media_type; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_media_type ON public.media USING btree (type);


--
-- Name: idx_media_uploaded; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_media_uploaded ON public.media USING btree (uploaded_at);


--
-- Name: idx_permission_tree; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_permission_tree ON public.tree_permissions USING btree (tree_id);


--
-- Name: idx_permission_user; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_permission_user ON public.tree_permissions USING btree (user_id);


--
-- Name: idx_relationship_ind1; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_relationship_ind1 ON public.relationships USING btree (individual1_id);


--
-- Name: idx_relationship_ind2; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_relationship_ind2 ON public.relationships USING btree (individual2_id);


--
-- Name: idx_relationship_tree; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_relationship_tree ON public.relationships USING btree (tree_id);


--
-- Name: idx_relationship_type; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_relationship_type ON public.relationships USING btree (type);


--
-- Name: idx_tree_admin; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_tree_admin ON public.family_trees USING btree (admin_id);


--
-- Name: idx_tree_created; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_tree_created ON public.family_trees USING btree (created_at);


--
-- Name: idx_tree_owner; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_tree_owner ON public.family_trees USING btree (owner_id);


--
-- Name: idx_tree_source_individual; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_tree_source_individual ON public.family_trees USING btree (source_individual_id);


--
-- Name: idx_user_email; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_user_email ON public.users USING btree (email);


--
-- Name: idx_user_username; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_user_username ON public.users USING btree (username);


--
-- Name: idx_utp_user_tree; Type: INDEX; Schema: public; Owner: familytree
--

CREATE INDEX idx_utp_user_tree ON public.user_tree_profiles USING btree (user_id, tree_id);


--
-- Name: user_tree_profiles fk1ghlwggnlv1rr2irn25omlvad; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.user_tree_profiles
    ADD CONSTRAINT fk1ghlwggnlv1rr2irn25omlvad FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: user_tree_profiles fk5qclo2e999o9lwhl6sai91jha; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.user_tree_profiles
    ADD CONSTRAINT fk5qclo2e999o9lwhl6sai91jha FOREIGN KEY (individual_id) REFERENCES public.individuals(id);


--
-- Name: media fk5vpnm7mb3a8gb3mhjokl2r00n; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.media
    ADD CONSTRAINT fk5vpnm7mb3a8gb3mhjokl2r00n FOREIGN KEY (individual_id) REFERENCES public.individuals(id);


--
-- Name: tree_invitations fk6djomccxemqrm3alg78xknhmw; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.tree_invitations
    ADD CONSTRAINT fk6djomccxemqrm3alg78xknhmw FOREIGN KEY (inviter_id) REFERENCES public.users(id);


--
-- Name: individuals fk6jt6t45dslomhnhph1cty9edc; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.individuals
    ADD CONSTRAINT fk6jt6t45dslomhnhph1cty9edc FOREIGN KEY (tree_id) REFERENCES public.family_trees(id);


--
-- Name: individual_clone_mappings fk9qt28arwycw611vxocf65ms49; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.individual_clone_mappings
    ADD CONSTRAINT fk9qt28arwycw611vxocf65ms49 FOREIGN KEY (source_tree_id) REFERENCES public.family_trees(id);


--
-- Name: tree_admins fka5gitolu66erwonfergcbcsjq; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.tree_admins
    ADD CONSTRAINT fka5gitolu66erwonfergcbcsjq FOREIGN KEY (tree_id) REFERENCES public.family_trees(id);


--
-- Name: relationships fkasdpaoi2me23ldf7d8rwyyd1s; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.relationships
    ADD CONSTRAINT fkasdpaoi2me23ldf7d8rwyyd1s FOREIGN KEY (tree_id) REFERENCES public.family_trees(id);


--
-- Name: family_trees fkbn9xjpl70c9e8x0m511c5koh8; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.family_trees
    ADD CONSTRAINT fkbn9xjpl70c9e8x0m511c5koh8 FOREIGN KEY (admin_id) REFERENCES public.users(id);


--
-- Name: relationships fkdhrk3clc67hgbqi66ylqxtf20; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.relationships
    ADD CONSTRAINT fkdhrk3clc67hgbqi66ylqxtf20 FOREIGN KEY (individual1_id) REFERENCES public.individuals(id);


--
-- Name: user_tree_profiles fkhcf77fb2lhophnki553f8ihuw; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.user_tree_profiles
    ADD CONSTRAINT fkhcf77fb2lhophnki553f8ihuw FOREIGN KEY (tree_id) REFERENCES public.family_trees(id);


--
-- Name: events fkhd5xg0je668obgdrr3kyn4q3u; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.events
    ADD CONSTRAINT fkhd5xg0je668obgdrr3kyn4q3u FOREIGN KEY (individual_id) REFERENCES public.individuals(id);


--
-- Name: individual_clone_mappings fkhlrcimt329ncdrbld34xvya41; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.individual_clone_mappings
    ADD CONSTRAINT fkhlrcimt329ncdrbld34xvya41 FOREIGN KEY (source_individual_id) REFERENCES public.individuals(id);


--
-- Name: tree_permissions fkhxjgd86vx1ra4hdkcu3f729md; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.tree_permissions
    ADD CONSTRAINT fkhxjgd86vx1ra4hdkcu3f729md FOREIGN KEY (tree_id) REFERENCES public.family_trees(id);


--
-- Name: tree_admins fkhyr49grjdb6euld8iolgj6nbr; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.tree_admins
    ADD CONSTRAINT fkhyr49grjdb6euld8iolgj6nbr FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: individual_clone_mappings fki68828lnhqa4w473ojihnr9wp; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.individual_clone_mappings
    ADD CONSTRAINT fki68828lnhqa4w473ojihnr9wp FOREIGN KEY (cloned_tree_id) REFERENCES public.family_trees(id);


--
-- Name: tree_permissions fkl3mv4svnbbqcyvh0n719xmnvx; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.tree_permissions
    ADD CONSTRAINT fkl3mv4svnbbqcyvh0n719xmnvx FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: family_trees fkq0nnclluayffil3ejoq8q5c5; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.family_trees
    ADD CONSTRAINT fkq0nnclluayffil3ejoq8q5c5 FOREIGN KEY (owner_id) REFERENCES public.users(id);


--
-- Name: individual_clone_mappings fkronlcnu7t1c3jpxb7xi6rxrlm; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.individual_clone_mappings
    ADD CONSTRAINT fkronlcnu7t1c3jpxb7xi6rxrlm FOREIGN KEY (cloned_individual_id) REFERENCES public.individuals(id);


--
-- Name: tree_invitations fkrp9wcuooy27bak9dmpe8524ur; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.tree_invitations
    ADD CONSTRAINT fkrp9wcuooy27bak9dmpe8524ur FOREIGN KEY (tree_id) REFERENCES public.family_trees(id);


--
-- Name: tree_invitations fksgbrl6van4hoe7vfxrywbqdbp; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.tree_invitations
    ADD CONSTRAINT fksgbrl6van4hoe7vfxrywbqdbp FOREIGN KEY (accepted_by_user_id) REFERENCES public.users(id);


--
-- Name: relationships fksm77eqey94mysr4j8xhvwbpta; Type: FK CONSTRAINT; Schema: public; Owner: familytree
--

ALTER TABLE ONLY public.relationships
    ADD CONSTRAINT fksm77eqey94mysr4j8xhvwbpta FOREIGN KEY (individual2_id) REFERENCES public.individuals(id);


--
-- PostgreSQL database dump complete
--

\unrestrict 1kxWCNxyItJqJEgCYtHhWw83mjwyhoE0qdfaPnBYlLEXICOzpHdc3CDwhBmHrHE

