-- NexIoT PostgreSQL 18; schema only; no source business rows.
--
-- PostgreSQL database dump
--



SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

--
-- Name: ai_model_capability ai_model_capability_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ai_model_capability
    ADD CONSTRAINT ai_model_capability_pkey PRIMARY KEY (id);


--
-- Name: ai_model_health ai_model_health_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ai_model_health
    ADD CONSTRAINT ai_model_health_pkey PRIMARY KEY (id);


--
-- Name: ai_model_profile ai_model_profile_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ai_model_profile
    ADD CONSTRAINT ai_model_profile_pkey PRIMARY KEY (id);


--
-- Name: ai_model_provider ai_model_provider_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ai_model_provider
    ADD CONSTRAINT ai_model_provider_pkey PRIMARY KEY (id);


--
-- Name: ai_prompt_template ai_prompt_template_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ai_prompt_template
    ADD CONSTRAINT ai_prompt_template_pkey PRIMARY KEY (id);


--
-- Name: code_protocol code_protocol_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.code_protocol
    ADD CONSTRAINT code_protocol_pkey PRIMARY KEY (code_key);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: gen_table_column gen_table_column_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.gen_table_column
    ADD CONSTRAINT gen_table_column_pkey PRIMARY KEY (column_id);


--
-- Name: gen_table gen_table_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.gen_table
    ADD CONSTRAINT gen_table_pkey PRIMARY KEY (table_id);


--
-- Name: iot_ai_document iot_ai_document_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ai_document
    ADD CONSTRAINT iot_ai_document_pkey PRIMARY KEY (id);


--
-- Name: iot_ai_draft iot_ai_draft_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ai_draft
    ADD CONSTRAINT iot_ai_draft_pkey PRIMARY KEY (id);


--
-- Name: iot_ai_provider iot_ai_provider_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ai_provider
    ADD CONSTRAINT iot_ai_provider_pkey PRIMARY KEY (id);


--
-- Name: iot_ai_resource iot_ai_resource_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ai_resource
    ADD CONSTRAINT iot_ai_resource_pkey PRIMARY KEY (id);


--
-- Name: iot_ai_run_log iot_ai_run_log_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ai_run_log
    ADD CONSTRAINT iot_ai_run_log_pkey PRIMARY KEY (id);


--
-- Name: iot_ai_session iot_ai_session_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ai_session
    ADD CONSTRAINT iot_ai_session_pkey PRIMARY KEY (id);


--
-- Name: iot_ai_skill iot_ai_skill_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ai_skill
    ADD CONSTRAINT iot_ai_skill_pkey PRIMARY KEY (id);


--
-- Name: iot_alarm_condition_state iot_alarm_condition_state_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_condition_state
    ADD CONSTRAINT iot_alarm_condition_state_pkey PRIMARY KEY (id);


--
-- Name: iot_alarm_evidence iot_alarm_evidence_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_evidence
    ADD CONSTRAINT iot_alarm_evidence_pkey PRIMARY KEY (id);


--
-- Name: iot_alarm_inbox iot_alarm_inbox_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_inbox
    ADD CONSTRAINT iot_alarm_inbox_pkey PRIMARY KEY (id);


--
-- Name: iot_alarm_key_lock iot_alarm_key_lock_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_key_lock
    ADD CONSTRAINT iot_alarm_key_lock_pkey PRIMARY KEY (id);


--
-- Name: iot_alarm_policy iot_alarm_policy_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_policy
    ADD CONSTRAINT iot_alarm_policy_pkey PRIMARY KEY (id);


--
-- Name: iot_alarm_record iot_alarm_record_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_record
    ADD CONSTRAINT iot_alarm_record_pkey PRIMARY KEY (id);


--
-- Name: iot_alarm_task iot_alarm_task_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_task
    ADD CONSTRAINT iot_alarm_task_pkey PRIMARY KEY (id);


--
-- Name: iot_alarm_timeline iot_alarm_timeline_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_timeline
    ADD CONSTRAINT iot_alarm_timeline_pkey PRIMARY KEY (id);


--
-- Name: iot_alarm_type iot_alarm_type_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_type
    ADD CONSTRAINT iot_alarm_type_pkey PRIMARY KEY (id);


--
-- Name: iot_alarm_work_order iot_alarm_work_order_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_work_order
    ADD CONSTRAINT iot_alarm_work_order_pkey PRIMARY KEY (id);


--
-- Name: iot_algorithm_alert_record iot_algorithm_alert_record_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_algorithm_alert_record
    ADD CONSTRAINT iot_algorithm_alert_record_pkey PRIMARY KEY (id);


--
-- Name: iot_algorithm_config iot_algorithm_config_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_algorithm_config
    ADD CONSTRAINT iot_algorithm_config_pkey PRIMARY KEY (id);


--
-- Name: iot_algorithm_result iot_algorithm_result_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_algorithm_result
    ADD CONSTRAINT iot_algorithm_result_pkey PRIMARY KEY (id);


--
-- Name: iot_analysis_page iot_analysis_page_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_analysis_page
    ADD CONSTRAINT iot_analysis_page_pkey PRIMARY KEY (id);


--
-- Name: iot_analysis_template_item iot_analysis_template_item_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_analysis_template_item
    ADD CONSTRAINT iot_analysis_template_item_pkey PRIMARY KEY (id);


--
-- Name: iot_analysis_template iot_analysis_template_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_analysis_template
    ADD CONSTRAINT iot_analysis_template_pkey PRIMARY KEY (id);


--
-- Name: iot_certificate iot_certificate_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_certificate
    ADD CONSTRAINT iot_certificate_pkey PRIMARY KEY (id);


--
-- Name: iot_dashboard_statistics iot_dashboard_statistics_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_dashboard_statistics
    ADD CONSTRAINT iot_dashboard_statistics_pkey PRIMARY KEY (id);


--
-- Name: iot_data_bridge_config iot_data_bridge_config_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_data_bridge_config
    ADD CONSTRAINT iot_data_bridge_config_pkey PRIMARY KEY (id);


--
-- Name: iot_data_input_log iot_data_input_log_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_data_input_log
    ADD CONSTRAINT iot_data_input_log_pkey PRIMARY KEY (id);


--
-- Name: iot_data_input_task iot_data_input_task_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_data_input_task
    ADD CONSTRAINT iot_data_input_task_pkey PRIMARY KEY (id);


--
-- Name: iot_device iot_device_copy1_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device
    ADD CONSTRAINT iot_device_copy1_pkey PRIMARY KEY (id);


--
-- Name: iot_device_fence_rel iot_device_fence_rel_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_fence_rel
    ADD CONSTRAINT iot_device_fence_rel_pkey PRIMARY KEY (id);


--
-- Name: iot_device_function_history iot_device_function_history_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_function_history
    ADD CONSTRAINT iot_device_function_history_pkey PRIMARY KEY (id);


--
-- Name: iot_device_function_task iot_device_function_task_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_function_task
    ADD CONSTRAINT iot_device_function_task_pkey PRIMARY KEY (id);


--
-- Name: iot_device_geo_fence iot_device_geo_fence_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_geo_fence
    ADD CONSTRAINT iot_device_geo_fence_pkey PRIMARY KEY (id);


--
-- Name: iot_device_group iot_device_group_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_group
    ADD CONSTRAINT iot_device_group_pkey PRIMARY KEY (id);


--
-- Name: iot_device_history iot_device_history_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_history
    ADD CONSTRAINT iot_device_history_pkey PRIMARY KEY (id);


--
-- Name: iot_device_log_0 iot_device_log_0_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_0
    ADD CONSTRAINT iot_device_log_0_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_0_p202603 iot_device_log_0_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_0_p202603
    ADD CONSTRAINT iot_device_log_0_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_0_p202604 iot_device_log_0_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_0_p202604
    ADD CONSTRAINT iot_device_log_0_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_0_p202605 iot_device_log_0_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_0_p202605
    ADD CONSTRAINT iot_device_log_0_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_0_p202606 iot_device_log_0_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_0_p202606
    ADD CONSTRAINT iot_device_log_0_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_0_p202607 iot_device_log_0_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_0_p202607
    ADD CONSTRAINT iot_device_log_0_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_0_p202608 iot_device_log_0_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_0_p202608
    ADD CONSTRAINT iot_device_log_0_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_0_p202609 iot_device_log_0_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_0_p202609
    ADD CONSTRAINT iot_device_log_0_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_10 iot_device_log_10_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_10
    ADD CONSTRAINT iot_device_log_10_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_10_p202603 iot_device_log_10_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_10_p202603
    ADD CONSTRAINT iot_device_log_10_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_10_p202604 iot_device_log_10_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_10_p202604
    ADD CONSTRAINT iot_device_log_10_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_10_p202605 iot_device_log_10_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_10_p202605
    ADD CONSTRAINT iot_device_log_10_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_10_p202606 iot_device_log_10_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_10_p202606
    ADD CONSTRAINT iot_device_log_10_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_10_p202607 iot_device_log_10_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_10_p202607
    ADD CONSTRAINT iot_device_log_10_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_10_p202608 iot_device_log_10_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_10_p202608
    ADD CONSTRAINT iot_device_log_10_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_10_p202609 iot_device_log_10_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_10_p202609
    ADD CONSTRAINT iot_device_log_10_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_11 iot_device_log_11_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_11
    ADD CONSTRAINT iot_device_log_11_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_11_p202603 iot_device_log_11_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_11_p202603
    ADD CONSTRAINT iot_device_log_11_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_11_p202604 iot_device_log_11_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_11_p202604
    ADD CONSTRAINT iot_device_log_11_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_11_p202605 iot_device_log_11_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_11_p202605
    ADD CONSTRAINT iot_device_log_11_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_11_p202606 iot_device_log_11_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_11_p202606
    ADD CONSTRAINT iot_device_log_11_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_11_p202607 iot_device_log_11_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_11_p202607
    ADD CONSTRAINT iot_device_log_11_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_11_p202608 iot_device_log_11_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_11_p202608
    ADD CONSTRAINT iot_device_log_11_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_11_p202609 iot_device_log_11_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_11_p202609
    ADD CONSTRAINT iot_device_log_11_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_12 iot_device_log_12_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_12
    ADD CONSTRAINT iot_device_log_12_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_12_p202603 iot_device_log_12_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_12_p202603
    ADD CONSTRAINT iot_device_log_12_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_12_p202604 iot_device_log_12_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_12_p202604
    ADD CONSTRAINT iot_device_log_12_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_12_p202605 iot_device_log_12_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_12_p202605
    ADD CONSTRAINT iot_device_log_12_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_12_p202606 iot_device_log_12_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_12_p202606
    ADD CONSTRAINT iot_device_log_12_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_12_p202607 iot_device_log_12_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_12_p202607
    ADD CONSTRAINT iot_device_log_12_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_12_p202608 iot_device_log_12_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_12_p202608
    ADD CONSTRAINT iot_device_log_12_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_12_p202609 iot_device_log_12_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_12_p202609
    ADD CONSTRAINT iot_device_log_12_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_13 iot_device_log_13_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_13
    ADD CONSTRAINT iot_device_log_13_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_13_p202603 iot_device_log_13_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_13_p202603
    ADD CONSTRAINT iot_device_log_13_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_13_p202604 iot_device_log_13_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_13_p202604
    ADD CONSTRAINT iot_device_log_13_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_13_p202605 iot_device_log_13_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_13_p202605
    ADD CONSTRAINT iot_device_log_13_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_13_p202606 iot_device_log_13_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_13_p202606
    ADD CONSTRAINT iot_device_log_13_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_13_p202607 iot_device_log_13_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_13_p202607
    ADD CONSTRAINT iot_device_log_13_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_13_p202608 iot_device_log_13_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_13_p202608
    ADD CONSTRAINT iot_device_log_13_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_13_p202609 iot_device_log_13_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_13_p202609
    ADD CONSTRAINT iot_device_log_13_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_14 iot_device_log_14_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_14
    ADD CONSTRAINT iot_device_log_14_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_14_p202603 iot_device_log_14_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_14_p202603
    ADD CONSTRAINT iot_device_log_14_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_14_p202604 iot_device_log_14_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_14_p202604
    ADD CONSTRAINT iot_device_log_14_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_14_p202605 iot_device_log_14_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_14_p202605
    ADD CONSTRAINT iot_device_log_14_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_14_p202606 iot_device_log_14_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_14_p202606
    ADD CONSTRAINT iot_device_log_14_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_14_p202607 iot_device_log_14_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_14_p202607
    ADD CONSTRAINT iot_device_log_14_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_14_p202608 iot_device_log_14_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_14_p202608
    ADD CONSTRAINT iot_device_log_14_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_14_p202609 iot_device_log_14_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_14_p202609
    ADD CONSTRAINT iot_device_log_14_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_15 iot_device_log_15_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_15
    ADD CONSTRAINT iot_device_log_15_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_15_p202603 iot_device_log_15_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_15_p202603
    ADD CONSTRAINT iot_device_log_15_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_15_p202604 iot_device_log_15_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_15_p202604
    ADD CONSTRAINT iot_device_log_15_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_15_p202605 iot_device_log_15_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_15_p202605
    ADD CONSTRAINT iot_device_log_15_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_15_p202606 iot_device_log_15_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_15_p202606
    ADD CONSTRAINT iot_device_log_15_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_15_p202607 iot_device_log_15_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_15_p202607
    ADD CONSTRAINT iot_device_log_15_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_15_p202608 iot_device_log_15_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_15_p202608
    ADD CONSTRAINT iot_device_log_15_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_15_p202609 iot_device_log_15_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_15_p202609
    ADD CONSTRAINT iot_device_log_15_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_16 iot_device_log_16_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_16
    ADD CONSTRAINT iot_device_log_16_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_16_p202603 iot_device_log_16_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_16_p202603
    ADD CONSTRAINT iot_device_log_16_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_16_p202604 iot_device_log_16_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_16_p202604
    ADD CONSTRAINT iot_device_log_16_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_16_p202605 iot_device_log_16_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_16_p202605
    ADD CONSTRAINT iot_device_log_16_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_16_p202606 iot_device_log_16_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_16_p202606
    ADD CONSTRAINT iot_device_log_16_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_16_p202607 iot_device_log_16_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_16_p202607
    ADD CONSTRAINT iot_device_log_16_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_16_p202608 iot_device_log_16_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_16_p202608
    ADD CONSTRAINT iot_device_log_16_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_16_p202609 iot_device_log_16_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_16_p202609
    ADD CONSTRAINT iot_device_log_16_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_17 iot_device_log_17_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_17
    ADD CONSTRAINT iot_device_log_17_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_17_p202603 iot_device_log_17_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_17_p202603
    ADD CONSTRAINT iot_device_log_17_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_17_p202604 iot_device_log_17_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_17_p202604
    ADD CONSTRAINT iot_device_log_17_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_17_p202605 iot_device_log_17_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_17_p202605
    ADD CONSTRAINT iot_device_log_17_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_17_p202606 iot_device_log_17_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_17_p202606
    ADD CONSTRAINT iot_device_log_17_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_17_p202607 iot_device_log_17_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_17_p202607
    ADD CONSTRAINT iot_device_log_17_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_17_p202608 iot_device_log_17_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_17_p202608
    ADD CONSTRAINT iot_device_log_17_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_17_p202609 iot_device_log_17_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_17_p202609
    ADD CONSTRAINT iot_device_log_17_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_18 iot_device_log_18_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_18
    ADD CONSTRAINT iot_device_log_18_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_18_p202603 iot_device_log_18_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_18_p202603
    ADD CONSTRAINT iot_device_log_18_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_18_p202604 iot_device_log_18_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_18_p202604
    ADD CONSTRAINT iot_device_log_18_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_18_p202605 iot_device_log_18_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_18_p202605
    ADD CONSTRAINT iot_device_log_18_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_18_p202606 iot_device_log_18_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_18_p202606
    ADD CONSTRAINT iot_device_log_18_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_18_p202607 iot_device_log_18_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_18_p202607
    ADD CONSTRAINT iot_device_log_18_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_18_p202608 iot_device_log_18_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_18_p202608
    ADD CONSTRAINT iot_device_log_18_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_18_p202609 iot_device_log_18_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_18_p202609
    ADD CONSTRAINT iot_device_log_18_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_19 iot_device_log_19_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_19
    ADD CONSTRAINT iot_device_log_19_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_19_p202603 iot_device_log_19_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_19_p202603
    ADD CONSTRAINT iot_device_log_19_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_19_p202604 iot_device_log_19_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_19_p202604
    ADD CONSTRAINT iot_device_log_19_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_19_p202605 iot_device_log_19_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_19_p202605
    ADD CONSTRAINT iot_device_log_19_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_19_p202606 iot_device_log_19_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_19_p202606
    ADD CONSTRAINT iot_device_log_19_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_19_p202607 iot_device_log_19_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_19_p202607
    ADD CONSTRAINT iot_device_log_19_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_19_p202608 iot_device_log_19_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_19_p202608
    ADD CONSTRAINT iot_device_log_19_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_19_p202609 iot_device_log_19_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_19_p202609
    ADD CONSTRAINT iot_device_log_19_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_1 iot_device_log_1_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_1
    ADD CONSTRAINT iot_device_log_1_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_1_p202603 iot_device_log_1_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_1_p202603
    ADD CONSTRAINT iot_device_log_1_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_1_p202604 iot_device_log_1_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_1_p202604
    ADD CONSTRAINT iot_device_log_1_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_1_p202605 iot_device_log_1_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_1_p202605
    ADD CONSTRAINT iot_device_log_1_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_1_p202606 iot_device_log_1_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_1_p202606
    ADD CONSTRAINT iot_device_log_1_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_1_p202607 iot_device_log_1_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_1_p202607
    ADD CONSTRAINT iot_device_log_1_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_1_p202608 iot_device_log_1_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_1_p202608
    ADD CONSTRAINT iot_device_log_1_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_1_p202609 iot_device_log_1_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_1_p202609
    ADD CONSTRAINT iot_device_log_1_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_20 iot_device_log_20_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_20
    ADD CONSTRAINT iot_device_log_20_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_20_p202603 iot_device_log_20_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_20_p202603
    ADD CONSTRAINT iot_device_log_20_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_20_p202604 iot_device_log_20_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_20_p202604
    ADD CONSTRAINT iot_device_log_20_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_20_p202605 iot_device_log_20_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_20_p202605
    ADD CONSTRAINT iot_device_log_20_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_20_p202606 iot_device_log_20_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_20_p202606
    ADD CONSTRAINT iot_device_log_20_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_20_p202607 iot_device_log_20_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_20_p202607
    ADD CONSTRAINT iot_device_log_20_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_20_p202608 iot_device_log_20_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_20_p202608
    ADD CONSTRAINT iot_device_log_20_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_20_p202609 iot_device_log_20_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_20_p202609
    ADD CONSTRAINT iot_device_log_20_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_21 iot_device_log_21_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_21
    ADD CONSTRAINT iot_device_log_21_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_21_p202603 iot_device_log_21_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_21_p202603
    ADD CONSTRAINT iot_device_log_21_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_21_p202604 iot_device_log_21_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_21_p202604
    ADD CONSTRAINT iot_device_log_21_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_21_p202605 iot_device_log_21_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_21_p202605
    ADD CONSTRAINT iot_device_log_21_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_21_p202606 iot_device_log_21_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_21_p202606
    ADD CONSTRAINT iot_device_log_21_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_21_p202607 iot_device_log_21_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_21_p202607
    ADD CONSTRAINT iot_device_log_21_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_21_p202608 iot_device_log_21_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_21_p202608
    ADD CONSTRAINT iot_device_log_21_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_21_p202609 iot_device_log_21_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_21_p202609
    ADD CONSTRAINT iot_device_log_21_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_22 iot_device_log_22_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_22
    ADD CONSTRAINT iot_device_log_22_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_22_p202603 iot_device_log_22_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_22_p202603
    ADD CONSTRAINT iot_device_log_22_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_22_p202604 iot_device_log_22_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_22_p202604
    ADD CONSTRAINT iot_device_log_22_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_22_p202605 iot_device_log_22_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_22_p202605
    ADD CONSTRAINT iot_device_log_22_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_22_p202606 iot_device_log_22_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_22_p202606
    ADD CONSTRAINT iot_device_log_22_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_22_p202607 iot_device_log_22_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_22_p202607
    ADD CONSTRAINT iot_device_log_22_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_22_p202608 iot_device_log_22_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_22_p202608
    ADD CONSTRAINT iot_device_log_22_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_22_p202609 iot_device_log_22_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_22_p202609
    ADD CONSTRAINT iot_device_log_22_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_23 iot_device_log_23_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_23
    ADD CONSTRAINT iot_device_log_23_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_23_p202603 iot_device_log_23_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_23_p202603
    ADD CONSTRAINT iot_device_log_23_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_23_p202604 iot_device_log_23_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_23_p202604
    ADD CONSTRAINT iot_device_log_23_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_23_p202605 iot_device_log_23_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_23_p202605
    ADD CONSTRAINT iot_device_log_23_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_23_p202606 iot_device_log_23_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_23_p202606
    ADD CONSTRAINT iot_device_log_23_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_23_p202607 iot_device_log_23_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_23_p202607
    ADD CONSTRAINT iot_device_log_23_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_23_p202608 iot_device_log_23_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_23_p202608
    ADD CONSTRAINT iot_device_log_23_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_23_p202609 iot_device_log_23_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_23_p202609
    ADD CONSTRAINT iot_device_log_23_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_2 iot_device_log_2_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_2
    ADD CONSTRAINT iot_device_log_2_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_2_p202603 iot_device_log_2_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_2_p202603
    ADD CONSTRAINT iot_device_log_2_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_2_p202604 iot_device_log_2_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_2_p202604
    ADD CONSTRAINT iot_device_log_2_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_2_p202605 iot_device_log_2_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_2_p202605
    ADD CONSTRAINT iot_device_log_2_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_2_p202606 iot_device_log_2_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_2_p202606
    ADD CONSTRAINT iot_device_log_2_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_2_p202607 iot_device_log_2_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_2_p202607
    ADD CONSTRAINT iot_device_log_2_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_2_p202608 iot_device_log_2_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_2_p202608
    ADD CONSTRAINT iot_device_log_2_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_2_p202609 iot_device_log_2_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_2_p202609
    ADD CONSTRAINT iot_device_log_2_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_3 iot_device_log_3_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_3
    ADD CONSTRAINT iot_device_log_3_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_3_p202603 iot_device_log_3_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_3_p202603
    ADD CONSTRAINT iot_device_log_3_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_3_p202604 iot_device_log_3_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_3_p202604
    ADD CONSTRAINT iot_device_log_3_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_3_p202605 iot_device_log_3_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_3_p202605
    ADD CONSTRAINT iot_device_log_3_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_3_p202606 iot_device_log_3_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_3_p202606
    ADD CONSTRAINT iot_device_log_3_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_3_p202607 iot_device_log_3_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_3_p202607
    ADD CONSTRAINT iot_device_log_3_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_3_p202608 iot_device_log_3_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_3_p202608
    ADD CONSTRAINT iot_device_log_3_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_3_p202609 iot_device_log_3_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_3_p202609
    ADD CONSTRAINT iot_device_log_3_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_4 iot_device_log_4_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_4
    ADD CONSTRAINT iot_device_log_4_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_4_p202603 iot_device_log_4_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_4_p202603
    ADD CONSTRAINT iot_device_log_4_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_4_p202604 iot_device_log_4_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_4_p202604
    ADD CONSTRAINT iot_device_log_4_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_4_p202605 iot_device_log_4_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_4_p202605
    ADD CONSTRAINT iot_device_log_4_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_4_p202606 iot_device_log_4_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_4_p202606
    ADD CONSTRAINT iot_device_log_4_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_4_p202607 iot_device_log_4_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_4_p202607
    ADD CONSTRAINT iot_device_log_4_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_4_p202608 iot_device_log_4_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_4_p202608
    ADD CONSTRAINT iot_device_log_4_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_4_p202609 iot_device_log_4_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_4_p202609
    ADD CONSTRAINT iot_device_log_4_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_5 iot_device_log_5_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_5
    ADD CONSTRAINT iot_device_log_5_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_5_p202603 iot_device_log_5_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_5_p202603
    ADD CONSTRAINT iot_device_log_5_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_5_p202604 iot_device_log_5_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_5_p202604
    ADD CONSTRAINT iot_device_log_5_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_5_p202605 iot_device_log_5_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_5_p202605
    ADD CONSTRAINT iot_device_log_5_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_5_p202606 iot_device_log_5_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_5_p202606
    ADD CONSTRAINT iot_device_log_5_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_5_p202607 iot_device_log_5_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_5_p202607
    ADD CONSTRAINT iot_device_log_5_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_5_p202608 iot_device_log_5_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_5_p202608
    ADD CONSTRAINT iot_device_log_5_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_5_p202609 iot_device_log_5_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_5_p202609
    ADD CONSTRAINT iot_device_log_5_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_6 iot_device_log_6_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_6
    ADD CONSTRAINT iot_device_log_6_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_6_p202603 iot_device_log_6_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_6_p202603
    ADD CONSTRAINT iot_device_log_6_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_6_p202604 iot_device_log_6_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_6_p202604
    ADD CONSTRAINT iot_device_log_6_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_6_p202605 iot_device_log_6_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_6_p202605
    ADD CONSTRAINT iot_device_log_6_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_6_p202606 iot_device_log_6_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_6_p202606
    ADD CONSTRAINT iot_device_log_6_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_6_p202607 iot_device_log_6_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_6_p202607
    ADD CONSTRAINT iot_device_log_6_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_6_p202608 iot_device_log_6_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_6_p202608
    ADD CONSTRAINT iot_device_log_6_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_6_p202609 iot_device_log_6_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_6_p202609
    ADD CONSTRAINT iot_device_log_6_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_7 iot_device_log_7_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_7
    ADD CONSTRAINT iot_device_log_7_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_7_p202603 iot_device_log_7_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_7_p202603
    ADD CONSTRAINT iot_device_log_7_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_7_p202604 iot_device_log_7_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_7_p202604
    ADD CONSTRAINT iot_device_log_7_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_7_p202605 iot_device_log_7_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_7_p202605
    ADD CONSTRAINT iot_device_log_7_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_7_p202606 iot_device_log_7_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_7_p202606
    ADD CONSTRAINT iot_device_log_7_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_7_p202607 iot_device_log_7_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_7_p202607
    ADD CONSTRAINT iot_device_log_7_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_7_p202608 iot_device_log_7_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_7_p202608
    ADD CONSTRAINT iot_device_log_7_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_7_p202609 iot_device_log_7_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_7_p202609
    ADD CONSTRAINT iot_device_log_7_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_8 iot_device_log_8_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_8
    ADD CONSTRAINT iot_device_log_8_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_8_p202603 iot_device_log_8_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_8_p202603
    ADD CONSTRAINT iot_device_log_8_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_8_p202604 iot_device_log_8_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_8_p202604
    ADD CONSTRAINT iot_device_log_8_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_8_p202605 iot_device_log_8_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_8_p202605
    ADD CONSTRAINT iot_device_log_8_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_8_p202606 iot_device_log_8_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_8_p202606
    ADD CONSTRAINT iot_device_log_8_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_8_p202607 iot_device_log_8_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_8_p202607
    ADD CONSTRAINT iot_device_log_8_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_8_p202608 iot_device_log_8_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_8_p202608
    ADD CONSTRAINT iot_device_log_8_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_8_p202609 iot_device_log_8_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_8_p202609
    ADD CONSTRAINT iot_device_log_8_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_9 iot_device_log_9_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_9
    ADD CONSTRAINT iot_device_log_9_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_9_p202603 iot_device_log_9_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_9_p202603
    ADD CONSTRAINT iot_device_log_9_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_9_p202604 iot_device_log_9_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_9_p202604
    ADD CONSTRAINT iot_device_log_9_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_9_p202605 iot_device_log_9_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_9_p202605
    ADD CONSTRAINT iot_device_log_9_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_9_p202606 iot_device_log_9_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_9_p202606
    ADD CONSTRAINT iot_device_log_9_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_9_p202607 iot_device_log_9_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_9_p202607
    ADD CONSTRAINT iot_device_log_9_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_9_p202608 iot_device_log_9_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_9_p202608
    ADD CONSTRAINT iot_device_log_9_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_9_p202609 iot_device_log_9_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_9_p202609
    ADD CONSTRAINT iot_device_log_9_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_0 iot_device_log_metadata_0_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_0
    ADD CONSTRAINT iot_device_log_metadata_0_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_0_p202603 iot_device_log_metadata_0_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_0_p202603
    ADD CONSTRAINT iot_device_log_metadata_0_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_0_p202604 iot_device_log_metadata_0_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_0_p202604
    ADD CONSTRAINT iot_device_log_metadata_0_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_0_p202605 iot_device_log_metadata_0_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_0_p202605
    ADD CONSTRAINT iot_device_log_metadata_0_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_0_p202606 iot_device_log_metadata_0_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_0_p202606
    ADD CONSTRAINT iot_device_log_metadata_0_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_0_p202607 iot_device_log_metadata_0_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_0_p202607
    ADD CONSTRAINT iot_device_log_metadata_0_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_0_p202608 iot_device_log_metadata_0_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_0_p202608
    ADD CONSTRAINT iot_device_log_metadata_0_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_0_p202609 iot_device_log_metadata_0_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_0_p202609
    ADD CONSTRAINT iot_device_log_metadata_0_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_10 iot_device_log_metadata_10_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_10
    ADD CONSTRAINT iot_device_log_metadata_10_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_10_p202603 iot_device_log_metadata_10_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_10_p202603
    ADD CONSTRAINT iot_device_log_metadata_10_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_10_p202604 iot_device_log_metadata_10_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_10_p202604
    ADD CONSTRAINT iot_device_log_metadata_10_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_10_p202605 iot_device_log_metadata_10_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_10_p202605
    ADD CONSTRAINT iot_device_log_metadata_10_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_10_p202606 iot_device_log_metadata_10_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_10_p202606
    ADD CONSTRAINT iot_device_log_metadata_10_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_10_p202607 iot_device_log_metadata_10_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_10_p202607
    ADD CONSTRAINT iot_device_log_metadata_10_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_10_p202608 iot_device_log_metadata_10_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_10_p202608
    ADD CONSTRAINT iot_device_log_metadata_10_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_10_p202609 iot_device_log_metadata_10_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_10_p202609
    ADD CONSTRAINT iot_device_log_metadata_10_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_11 iot_device_log_metadata_11_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_11
    ADD CONSTRAINT iot_device_log_metadata_11_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_11_p202603 iot_device_log_metadata_11_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_11_p202603
    ADD CONSTRAINT iot_device_log_metadata_11_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_11_p202604 iot_device_log_metadata_11_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_11_p202604
    ADD CONSTRAINT iot_device_log_metadata_11_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_11_p202605 iot_device_log_metadata_11_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_11_p202605
    ADD CONSTRAINT iot_device_log_metadata_11_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_11_p202606 iot_device_log_metadata_11_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_11_p202606
    ADD CONSTRAINT iot_device_log_metadata_11_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_11_p202607 iot_device_log_metadata_11_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_11_p202607
    ADD CONSTRAINT iot_device_log_metadata_11_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_11_p202608 iot_device_log_metadata_11_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_11_p202608
    ADD CONSTRAINT iot_device_log_metadata_11_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_11_p202609 iot_device_log_metadata_11_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_11_p202609
    ADD CONSTRAINT iot_device_log_metadata_11_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_12 iot_device_log_metadata_12_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_12
    ADD CONSTRAINT iot_device_log_metadata_12_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_12_p202603 iot_device_log_metadata_12_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_12_p202603
    ADD CONSTRAINT iot_device_log_metadata_12_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_12_p202604 iot_device_log_metadata_12_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_12_p202604
    ADD CONSTRAINT iot_device_log_metadata_12_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_12_p202605 iot_device_log_metadata_12_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_12_p202605
    ADD CONSTRAINT iot_device_log_metadata_12_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_12_p202606 iot_device_log_metadata_12_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_12_p202606
    ADD CONSTRAINT iot_device_log_metadata_12_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_12_p202607 iot_device_log_metadata_12_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_12_p202607
    ADD CONSTRAINT iot_device_log_metadata_12_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_12_p202608 iot_device_log_metadata_12_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_12_p202608
    ADD CONSTRAINT iot_device_log_metadata_12_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_12_p202609 iot_device_log_metadata_12_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_12_p202609
    ADD CONSTRAINT iot_device_log_metadata_12_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_13 iot_device_log_metadata_13_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_13
    ADD CONSTRAINT iot_device_log_metadata_13_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_13_p202603 iot_device_log_metadata_13_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_13_p202603
    ADD CONSTRAINT iot_device_log_metadata_13_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_13_p202604 iot_device_log_metadata_13_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_13_p202604
    ADD CONSTRAINT iot_device_log_metadata_13_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_13_p202605 iot_device_log_metadata_13_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_13_p202605
    ADD CONSTRAINT iot_device_log_metadata_13_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_13_p202606 iot_device_log_metadata_13_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_13_p202606
    ADD CONSTRAINT iot_device_log_metadata_13_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_13_p202607 iot_device_log_metadata_13_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_13_p202607
    ADD CONSTRAINT iot_device_log_metadata_13_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_13_p202608 iot_device_log_metadata_13_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_13_p202608
    ADD CONSTRAINT iot_device_log_metadata_13_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_13_p202609 iot_device_log_metadata_13_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_13_p202609
    ADD CONSTRAINT iot_device_log_metadata_13_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_14 iot_device_log_metadata_14_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_14
    ADD CONSTRAINT iot_device_log_metadata_14_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_14_p202603 iot_device_log_metadata_14_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_14_p202603
    ADD CONSTRAINT iot_device_log_metadata_14_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_14_p202604 iot_device_log_metadata_14_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_14_p202604
    ADD CONSTRAINT iot_device_log_metadata_14_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_14_p202605 iot_device_log_metadata_14_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_14_p202605
    ADD CONSTRAINT iot_device_log_metadata_14_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_14_p202606 iot_device_log_metadata_14_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_14_p202606
    ADD CONSTRAINT iot_device_log_metadata_14_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_14_p202607 iot_device_log_metadata_14_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_14_p202607
    ADD CONSTRAINT iot_device_log_metadata_14_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_14_p202608 iot_device_log_metadata_14_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_14_p202608
    ADD CONSTRAINT iot_device_log_metadata_14_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_14_p202609 iot_device_log_metadata_14_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_14_p202609
    ADD CONSTRAINT iot_device_log_metadata_14_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_15 iot_device_log_metadata_15_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_15
    ADD CONSTRAINT iot_device_log_metadata_15_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_15_p202603 iot_device_log_metadata_15_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_15_p202603
    ADD CONSTRAINT iot_device_log_metadata_15_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_15_p202604 iot_device_log_metadata_15_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_15_p202604
    ADD CONSTRAINT iot_device_log_metadata_15_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_15_p202605 iot_device_log_metadata_15_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_15_p202605
    ADD CONSTRAINT iot_device_log_metadata_15_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_15_p202606 iot_device_log_metadata_15_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_15_p202606
    ADD CONSTRAINT iot_device_log_metadata_15_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_15_p202607 iot_device_log_metadata_15_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_15_p202607
    ADD CONSTRAINT iot_device_log_metadata_15_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_15_p202608 iot_device_log_metadata_15_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_15_p202608
    ADD CONSTRAINT iot_device_log_metadata_15_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_15_p202609 iot_device_log_metadata_15_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_15_p202609
    ADD CONSTRAINT iot_device_log_metadata_15_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_16 iot_device_log_metadata_16_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_16
    ADD CONSTRAINT iot_device_log_metadata_16_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_16_p202603 iot_device_log_metadata_16_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_16_p202603
    ADD CONSTRAINT iot_device_log_metadata_16_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_16_p202604 iot_device_log_metadata_16_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_16_p202604
    ADD CONSTRAINT iot_device_log_metadata_16_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_16_p202605 iot_device_log_metadata_16_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_16_p202605
    ADD CONSTRAINT iot_device_log_metadata_16_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_16_p202606 iot_device_log_metadata_16_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_16_p202606
    ADD CONSTRAINT iot_device_log_metadata_16_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_16_p202607 iot_device_log_metadata_16_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_16_p202607
    ADD CONSTRAINT iot_device_log_metadata_16_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_16_p202608 iot_device_log_metadata_16_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_16_p202608
    ADD CONSTRAINT iot_device_log_metadata_16_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_16_p202609 iot_device_log_metadata_16_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_16_p202609
    ADD CONSTRAINT iot_device_log_metadata_16_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_17 iot_device_log_metadata_17_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_17
    ADD CONSTRAINT iot_device_log_metadata_17_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_17_p202603 iot_device_log_metadata_17_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_17_p202603
    ADD CONSTRAINT iot_device_log_metadata_17_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_17_p202604 iot_device_log_metadata_17_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_17_p202604
    ADD CONSTRAINT iot_device_log_metadata_17_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_17_p202605 iot_device_log_metadata_17_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_17_p202605
    ADD CONSTRAINT iot_device_log_metadata_17_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_17_p202606 iot_device_log_metadata_17_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_17_p202606
    ADD CONSTRAINT iot_device_log_metadata_17_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_17_p202607 iot_device_log_metadata_17_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_17_p202607
    ADD CONSTRAINT iot_device_log_metadata_17_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_17_p202608 iot_device_log_metadata_17_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_17_p202608
    ADD CONSTRAINT iot_device_log_metadata_17_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_17_p202609 iot_device_log_metadata_17_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_17_p202609
    ADD CONSTRAINT iot_device_log_metadata_17_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_18 iot_device_log_metadata_18_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_18
    ADD CONSTRAINT iot_device_log_metadata_18_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_18_p202603 iot_device_log_metadata_18_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_18_p202603
    ADD CONSTRAINT iot_device_log_metadata_18_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_18_p202604 iot_device_log_metadata_18_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_18_p202604
    ADD CONSTRAINT iot_device_log_metadata_18_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_18_p202605 iot_device_log_metadata_18_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_18_p202605
    ADD CONSTRAINT iot_device_log_metadata_18_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_18_p202606 iot_device_log_metadata_18_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_18_p202606
    ADD CONSTRAINT iot_device_log_metadata_18_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_18_p202607 iot_device_log_metadata_18_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_18_p202607
    ADD CONSTRAINT iot_device_log_metadata_18_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_18_p202608 iot_device_log_metadata_18_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_18_p202608
    ADD CONSTRAINT iot_device_log_metadata_18_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_18_p202609 iot_device_log_metadata_18_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_18_p202609
    ADD CONSTRAINT iot_device_log_metadata_18_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_19 iot_device_log_metadata_19_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_19
    ADD CONSTRAINT iot_device_log_metadata_19_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_19_p202603 iot_device_log_metadata_19_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_19_p202603
    ADD CONSTRAINT iot_device_log_metadata_19_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_19_p202604 iot_device_log_metadata_19_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_19_p202604
    ADD CONSTRAINT iot_device_log_metadata_19_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_19_p202605 iot_device_log_metadata_19_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_19_p202605
    ADD CONSTRAINT iot_device_log_metadata_19_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_19_p202606 iot_device_log_metadata_19_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_19_p202606
    ADD CONSTRAINT iot_device_log_metadata_19_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_19_p202607 iot_device_log_metadata_19_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_19_p202607
    ADD CONSTRAINT iot_device_log_metadata_19_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_19_p202608 iot_device_log_metadata_19_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_19_p202608
    ADD CONSTRAINT iot_device_log_metadata_19_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_19_p202609 iot_device_log_metadata_19_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_19_p202609
    ADD CONSTRAINT iot_device_log_metadata_19_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_1 iot_device_log_metadata_1_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_1
    ADD CONSTRAINT iot_device_log_metadata_1_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_1_p202603 iot_device_log_metadata_1_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_1_p202603
    ADD CONSTRAINT iot_device_log_metadata_1_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_1_p202604 iot_device_log_metadata_1_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_1_p202604
    ADD CONSTRAINT iot_device_log_metadata_1_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_1_p202605 iot_device_log_metadata_1_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_1_p202605
    ADD CONSTRAINT iot_device_log_metadata_1_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_1_p202606 iot_device_log_metadata_1_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_1_p202606
    ADD CONSTRAINT iot_device_log_metadata_1_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_1_p202607 iot_device_log_metadata_1_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_1_p202607
    ADD CONSTRAINT iot_device_log_metadata_1_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_1_p202608 iot_device_log_metadata_1_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_1_p202608
    ADD CONSTRAINT iot_device_log_metadata_1_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_1_p202609 iot_device_log_metadata_1_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_1_p202609
    ADD CONSTRAINT iot_device_log_metadata_1_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_20 iot_device_log_metadata_20_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_20
    ADD CONSTRAINT iot_device_log_metadata_20_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_20_p202603 iot_device_log_metadata_20_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_20_p202603
    ADD CONSTRAINT iot_device_log_metadata_20_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_20_p202604 iot_device_log_metadata_20_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_20_p202604
    ADD CONSTRAINT iot_device_log_metadata_20_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_20_p202605 iot_device_log_metadata_20_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_20_p202605
    ADD CONSTRAINT iot_device_log_metadata_20_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_20_p202606 iot_device_log_metadata_20_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_20_p202606
    ADD CONSTRAINT iot_device_log_metadata_20_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_20_p202607 iot_device_log_metadata_20_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_20_p202607
    ADD CONSTRAINT iot_device_log_metadata_20_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_20_p202608 iot_device_log_metadata_20_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_20_p202608
    ADD CONSTRAINT iot_device_log_metadata_20_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_20_p202609 iot_device_log_metadata_20_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_20_p202609
    ADD CONSTRAINT iot_device_log_metadata_20_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_21 iot_device_log_metadata_21_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_21
    ADD CONSTRAINT iot_device_log_metadata_21_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_21_p202603 iot_device_log_metadata_21_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_21_p202603
    ADD CONSTRAINT iot_device_log_metadata_21_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_21_p202604 iot_device_log_metadata_21_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_21_p202604
    ADD CONSTRAINT iot_device_log_metadata_21_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_21_p202605 iot_device_log_metadata_21_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_21_p202605
    ADD CONSTRAINT iot_device_log_metadata_21_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_21_p202606 iot_device_log_metadata_21_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_21_p202606
    ADD CONSTRAINT iot_device_log_metadata_21_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_21_p202607 iot_device_log_metadata_21_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_21_p202607
    ADD CONSTRAINT iot_device_log_metadata_21_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_21_p202608 iot_device_log_metadata_21_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_21_p202608
    ADD CONSTRAINT iot_device_log_metadata_21_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_21_p202609 iot_device_log_metadata_21_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_21_p202609
    ADD CONSTRAINT iot_device_log_metadata_21_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_22 iot_device_log_metadata_22_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_22
    ADD CONSTRAINT iot_device_log_metadata_22_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_22_p202603 iot_device_log_metadata_22_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_22_p202603
    ADD CONSTRAINT iot_device_log_metadata_22_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_22_p202604 iot_device_log_metadata_22_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_22_p202604
    ADD CONSTRAINT iot_device_log_metadata_22_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_22_p202605 iot_device_log_metadata_22_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_22_p202605
    ADD CONSTRAINT iot_device_log_metadata_22_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_22_p202606 iot_device_log_metadata_22_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_22_p202606
    ADD CONSTRAINT iot_device_log_metadata_22_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_22_p202607 iot_device_log_metadata_22_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_22_p202607
    ADD CONSTRAINT iot_device_log_metadata_22_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_22_p202608 iot_device_log_metadata_22_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_22_p202608
    ADD CONSTRAINT iot_device_log_metadata_22_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_22_p202609 iot_device_log_metadata_22_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_22_p202609
    ADD CONSTRAINT iot_device_log_metadata_22_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_23 iot_device_log_metadata_23_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_23
    ADD CONSTRAINT iot_device_log_metadata_23_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_23_p202603 iot_device_log_metadata_23_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_23_p202603
    ADD CONSTRAINT iot_device_log_metadata_23_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_23_p202604 iot_device_log_metadata_23_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_23_p202604
    ADD CONSTRAINT iot_device_log_metadata_23_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_23_p202605 iot_device_log_metadata_23_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_23_p202605
    ADD CONSTRAINT iot_device_log_metadata_23_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_23_p202606 iot_device_log_metadata_23_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_23_p202606
    ADD CONSTRAINT iot_device_log_metadata_23_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_23_p202607 iot_device_log_metadata_23_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_23_p202607
    ADD CONSTRAINT iot_device_log_metadata_23_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_23_p202608 iot_device_log_metadata_23_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_23_p202608
    ADD CONSTRAINT iot_device_log_metadata_23_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_23_p202609 iot_device_log_metadata_23_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_23_p202609
    ADD CONSTRAINT iot_device_log_metadata_23_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_2 iot_device_log_metadata_2_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_2
    ADD CONSTRAINT iot_device_log_metadata_2_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_2_p202603 iot_device_log_metadata_2_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_2_p202603
    ADD CONSTRAINT iot_device_log_metadata_2_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_2_p202604 iot_device_log_metadata_2_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_2_p202604
    ADD CONSTRAINT iot_device_log_metadata_2_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_2_p202605 iot_device_log_metadata_2_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_2_p202605
    ADD CONSTRAINT iot_device_log_metadata_2_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_2_p202606 iot_device_log_metadata_2_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_2_p202606
    ADD CONSTRAINT iot_device_log_metadata_2_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_2_p202607 iot_device_log_metadata_2_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_2_p202607
    ADD CONSTRAINT iot_device_log_metadata_2_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_2_p202608 iot_device_log_metadata_2_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_2_p202608
    ADD CONSTRAINT iot_device_log_metadata_2_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_2_p202609 iot_device_log_metadata_2_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_2_p202609
    ADD CONSTRAINT iot_device_log_metadata_2_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_3 iot_device_log_metadata_3_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_3
    ADD CONSTRAINT iot_device_log_metadata_3_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_3_p202603 iot_device_log_metadata_3_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_3_p202603
    ADD CONSTRAINT iot_device_log_metadata_3_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_3_p202604 iot_device_log_metadata_3_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_3_p202604
    ADD CONSTRAINT iot_device_log_metadata_3_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_3_p202605 iot_device_log_metadata_3_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_3_p202605
    ADD CONSTRAINT iot_device_log_metadata_3_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_3_p202606 iot_device_log_metadata_3_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_3_p202606
    ADD CONSTRAINT iot_device_log_metadata_3_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_3_p202607 iot_device_log_metadata_3_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_3_p202607
    ADD CONSTRAINT iot_device_log_metadata_3_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_3_p202608 iot_device_log_metadata_3_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_3_p202608
    ADD CONSTRAINT iot_device_log_metadata_3_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_3_p202609 iot_device_log_metadata_3_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_3_p202609
    ADD CONSTRAINT iot_device_log_metadata_3_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_4 iot_device_log_metadata_4_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_4
    ADD CONSTRAINT iot_device_log_metadata_4_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_4_p202603 iot_device_log_metadata_4_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_4_p202603
    ADD CONSTRAINT iot_device_log_metadata_4_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_4_p202604 iot_device_log_metadata_4_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_4_p202604
    ADD CONSTRAINT iot_device_log_metadata_4_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_4_p202605 iot_device_log_metadata_4_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_4_p202605
    ADD CONSTRAINT iot_device_log_metadata_4_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_4_p202606 iot_device_log_metadata_4_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_4_p202606
    ADD CONSTRAINT iot_device_log_metadata_4_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_4_p202607 iot_device_log_metadata_4_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_4_p202607
    ADD CONSTRAINT iot_device_log_metadata_4_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_4_p202608 iot_device_log_metadata_4_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_4_p202608
    ADD CONSTRAINT iot_device_log_metadata_4_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_4_p202609 iot_device_log_metadata_4_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_4_p202609
    ADD CONSTRAINT iot_device_log_metadata_4_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_5 iot_device_log_metadata_5_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_5
    ADD CONSTRAINT iot_device_log_metadata_5_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_5_p202603 iot_device_log_metadata_5_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_5_p202603
    ADD CONSTRAINT iot_device_log_metadata_5_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_5_p202604 iot_device_log_metadata_5_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_5_p202604
    ADD CONSTRAINT iot_device_log_metadata_5_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_5_p202605 iot_device_log_metadata_5_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_5_p202605
    ADD CONSTRAINT iot_device_log_metadata_5_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_5_p202606 iot_device_log_metadata_5_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_5_p202606
    ADD CONSTRAINT iot_device_log_metadata_5_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_5_p202607 iot_device_log_metadata_5_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_5_p202607
    ADD CONSTRAINT iot_device_log_metadata_5_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_5_p202608 iot_device_log_metadata_5_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_5_p202608
    ADD CONSTRAINT iot_device_log_metadata_5_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_5_p202609 iot_device_log_metadata_5_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_5_p202609
    ADD CONSTRAINT iot_device_log_metadata_5_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_6 iot_device_log_metadata_6_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_6
    ADD CONSTRAINT iot_device_log_metadata_6_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_6_p202603 iot_device_log_metadata_6_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_6_p202603
    ADD CONSTRAINT iot_device_log_metadata_6_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_6_p202604 iot_device_log_metadata_6_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_6_p202604
    ADD CONSTRAINT iot_device_log_metadata_6_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_6_p202605 iot_device_log_metadata_6_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_6_p202605
    ADD CONSTRAINT iot_device_log_metadata_6_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_6_p202606 iot_device_log_metadata_6_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_6_p202606
    ADD CONSTRAINT iot_device_log_metadata_6_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_6_p202607 iot_device_log_metadata_6_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_6_p202607
    ADD CONSTRAINT iot_device_log_metadata_6_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_6_p202608 iot_device_log_metadata_6_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_6_p202608
    ADD CONSTRAINT iot_device_log_metadata_6_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_6_p202609 iot_device_log_metadata_6_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_6_p202609
    ADD CONSTRAINT iot_device_log_metadata_6_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_7 iot_device_log_metadata_7_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_7
    ADD CONSTRAINT iot_device_log_metadata_7_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_7_p202603 iot_device_log_metadata_7_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_7_p202603
    ADD CONSTRAINT iot_device_log_metadata_7_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_7_p202604 iot_device_log_metadata_7_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_7_p202604
    ADD CONSTRAINT iot_device_log_metadata_7_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_7_p202605 iot_device_log_metadata_7_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_7_p202605
    ADD CONSTRAINT iot_device_log_metadata_7_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_7_p202606 iot_device_log_metadata_7_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_7_p202606
    ADD CONSTRAINT iot_device_log_metadata_7_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_7_p202607 iot_device_log_metadata_7_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_7_p202607
    ADD CONSTRAINT iot_device_log_metadata_7_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_7_p202608 iot_device_log_metadata_7_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_7_p202608
    ADD CONSTRAINT iot_device_log_metadata_7_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_7_p202609 iot_device_log_metadata_7_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_7_p202609
    ADD CONSTRAINT iot_device_log_metadata_7_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_8 iot_device_log_metadata_8_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_8
    ADD CONSTRAINT iot_device_log_metadata_8_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_8_p202603 iot_device_log_metadata_8_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_8_p202603
    ADD CONSTRAINT iot_device_log_metadata_8_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_8_p202604 iot_device_log_metadata_8_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_8_p202604
    ADD CONSTRAINT iot_device_log_metadata_8_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_8_p202605 iot_device_log_metadata_8_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_8_p202605
    ADD CONSTRAINT iot_device_log_metadata_8_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_8_p202606 iot_device_log_metadata_8_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_8_p202606
    ADD CONSTRAINT iot_device_log_metadata_8_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_8_p202607 iot_device_log_metadata_8_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_8_p202607
    ADD CONSTRAINT iot_device_log_metadata_8_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_8_p202608 iot_device_log_metadata_8_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_8_p202608
    ADD CONSTRAINT iot_device_log_metadata_8_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_8_p202609 iot_device_log_metadata_8_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_8_p202609
    ADD CONSTRAINT iot_device_log_metadata_8_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_9 iot_device_log_metadata_9_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_9
    ADD CONSTRAINT iot_device_log_metadata_9_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_9_p202603 iot_device_log_metadata_9_p202603_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_9_p202603
    ADD CONSTRAINT iot_device_log_metadata_9_p202603_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_9_p202604 iot_device_log_metadata_9_p202604_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_9_p202604
    ADD CONSTRAINT iot_device_log_metadata_9_p202604_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_9_p202605 iot_device_log_metadata_9_p202605_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_9_p202605
    ADD CONSTRAINT iot_device_log_metadata_9_p202605_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_9_p202606 iot_device_log_metadata_9_p202606_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_9_p202606
    ADD CONSTRAINT iot_device_log_metadata_9_p202606_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_9_p202607 iot_device_log_metadata_9_p202607_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_9_p202607
    ADD CONSTRAINT iot_device_log_metadata_9_p202607_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_9_p202608 iot_device_log_metadata_9_p202608_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_9_p202608
    ADD CONSTRAINT iot_device_log_metadata_9_p202608_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata_9_p202609 iot_device_log_metadata_9_p202609_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata_9_p202609
    ADD CONSTRAINT iot_device_log_metadata_9_p202609_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_device_log_metadata iot_device_log_metadata_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log_metadata
    ADD CONSTRAINT iot_device_log_metadata_pkey PRIMARY KEY (id);


--
-- Name: iot_device_log iot_device_log_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_log
    ADD CONSTRAINT iot_device_log_pkey PRIMARY KEY (id);


--
-- Name: iot_device_model iot_device_model_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_model
    ADD CONSTRAINT iot_device_model_pkey PRIMARY KEY (id);


--
-- Name: iot_device_protocol iot_device_protocol_copy1_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_protocol
    ADD CONSTRAINT iot_device_protocol_copy1_pkey PRIMARY KEY (id);


--
-- Name: iot_device_protocol1 iot_device_protocol_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_protocol1
    ADD CONSTRAINT iot_device_protocol_pkey PRIMARY KEY (id);


--
-- Name: iot_device_rule_log iot_device_rule_log_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_rule_log
    ADD CONSTRAINT iot_device_rule_log_pkey PRIMARY KEY (id);


--
-- Name: iot_device_shadow iot_device_shadow_copy1_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_shadow
    ADD CONSTRAINT iot_device_shadow_copy1_pkey PRIMARY KEY (id);


--
-- Name: iot_device_statistics iot_device_statistics_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_statistics
    ADD CONSTRAINT iot_device_statistics_pkey PRIMARY KEY (id);


--
-- Name: iot_device_subscribe iot_device_subscribe_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_subscribe
    ADD CONSTRAINT iot_device_subscribe_pkey PRIMARY KEY (id);


--
-- Name: iot_device_tags iot_device_tags_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_tags
    ADD CONSTRAINT iot_device_tags_pkey PRIMARY KEY (id);


--
-- Name: iot_device_up_format iot_device_up_format_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_device_up_format
    ADD CONSTRAINT iot_device_up_format_pkey PRIMARY KEY (id);


--
-- Name: iot_flow_definition iot_flow_definition_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_flow_definition
    ADD CONSTRAINT iot_flow_definition_pkey PRIMARY KEY (id);


--
-- Name: iot_flow_deployment iot_flow_deployment_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_flow_deployment
    ADD CONSTRAINT iot_flow_deployment_pkey PRIMARY KEY (tenant_id, definition_id);


--
-- Name: iot_flow_execution iot_flow_execution_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_flow_execution
    ADD CONSTRAINT iot_flow_execution_pkey PRIMARY KEY (id);


--
-- Name: iot_flow_version iot_flow_version_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_flow_version
    ADD CONSTRAINT iot_flow_version_pkey PRIMARY KEY (id);


--
-- Name: iot_flow_version iot_flow_version_tenant_id_definition_id_version_no_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_flow_version
    ADD CONSTRAINT iot_flow_version_tenant_id_definition_id_version_no_key UNIQUE (tenant_id, definition_id, version_no);


--
-- Name: iot_gateway_polling_command iot_gateway_polling_command_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_gateway_polling_command
    ADD CONSTRAINT iot_gateway_polling_command_pkey PRIMARY KEY (id);


--
-- Name: iot_gateway_polling_config iot_gateway_polling_config_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_gateway_polling_config
    ADD CONSTRAINT iot_gateway_polling_config_pkey PRIMARY KEY (id);


--
-- Name: iot_industrial_asset iot_industrial_asset_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_asset
    ADD CONSTRAINT iot_industrial_asset_pkey PRIMARY KEY (id);


--
-- Name: iot_industrial_collect_group iot_industrial_collect_group_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_collect_group
    ADD CONSTRAINT iot_industrial_collect_group_pkey PRIMARY KEY (id);


--
-- Name: iot_industrial_collect_point iot_industrial_collect_point_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_collect_point
    ADD CONSTRAINT iot_industrial_collect_point_pkey PRIMARY KEY (id);


--
-- Name: iot_industrial_config_import_preview iot_industrial_config_import_preview_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_config_import_preview
    ADD CONSTRAINT iot_industrial_config_import_preview_pkey PRIMARY KEY (id);


--
-- Name: iot_industrial_config_template iot_industrial_config_template_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_config_template
    ADD CONSTRAINT iot_industrial_config_template_pkey PRIMARY KEY (id);


--
-- Name: iot_industrial_config_template_version iot_industrial_config_template_version_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_config_template_version
    ADD CONSTRAINT iot_industrial_config_template_version_pkey PRIMARY KEY (id);


--
-- Name: iot_industrial_endpoint iot_industrial_endpoint_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_endpoint
    ADD CONSTRAINT iot_industrial_endpoint_pkey PRIMARY KEY (id);


--
-- Name: iot_industrial_point iot_industrial_point_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_point
    ADD CONSTRAINT iot_industrial_point_pkey PRIMARY KEY (id);


--
-- Name: iot_industrial_runtime_stat iot_industrial_runtime_stat_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_runtime_stat
    ADD CONSTRAINT iot_industrial_runtime_stat_pkey PRIMARY KEY (id);


--
-- Name: iot_network iot_network_copy1_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_network
    ADD CONSTRAINT iot_network_copy1_pkey PRIMARY KEY (id);


--
-- Name: iot_network1 iot_network_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_network1
    ADD CONSTRAINT iot_network_pkey PRIMARY KEY (id);


--
-- Name: iot_node_red_binding iot_node_red_binding_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_node_red_binding
    ADD CONSTRAINT iot_node_red_binding_pkey PRIMARY KEY (id);


--
-- Name: iot_node_red_credential iot_node_red_credential_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_node_red_credential
    ADD CONSTRAINT iot_node_red_credential_pkey PRIMARY KEY (id);


--
-- Name: iot_node_red_deployment_token iot_node_red_deployment_token_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_node_red_deployment_token
    ADD CONSTRAINT iot_node_red_deployment_token_pkey PRIMARY KEY (id);


--
-- Name: iot_node_red_flow_deployment iot_node_red_flow_deployment_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_node_red_flow_deployment
    ADD CONSTRAINT iot_node_red_flow_deployment_pkey PRIMARY KEY (id);


--
-- Name: iot_node_red_flow_snapshot iot_node_red_flow_snapshot_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_node_red_flow_snapshot
    ADD CONSTRAINT iot_node_red_flow_snapshot_pkey PRIMARY KEY (id);


--
-- Name: iot_node_red_flow_template_category iot_node_red_flow_template_category_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_node_red_flow_template_category
    ADD CONSTRAINT iot_node_red_flow_template_category_pkey PRIMARY KEY (id);


--
-- Name: iot_node_red_flow_template iot_node_red_flow_template_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_node_red_flow_template
    ADD CONSTRAINT iot_node_red_flow_template_pkey PRIMARY KEY (id);


--
-- Name: iot_node_red_instance iot_node_red_instance_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_node_red_instance
    ADD CONSTRAINT iot_node_red_instance_pkey PRIMARY KEY (id);


--
-- Name: iot_ota_device_version iot_ota_device_version_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ota_device_version
    ADD CONSTRAINT iot_ota_device_version_pkey PRIMARY KEY (id);


--
-- Name: iot_ota_package iot_ota_package_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ota_package
    ADD CONSTRAINT iot_ota_package_pkey PRIMARY KEY (id);


--
-- Name: iot_ota_package_tag_rel iot_ota_package_tag_rel_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ota_package_tag_rel
    ADD CONSTRAINT iot_ota_package_tag_rel_pkey PRIMARY KEY (id);


--
-- Name: iot_ota_tag iot_ota_tag_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ota_tag
    ADD CONSTRAINT iot_ota_tag_pkey PRIMARY KEY (id);


--
-- Name: iot_ota_task_device iot_ota_task_device_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ota_task_device
    ADD CONSTRAINT iot_ota_task_device_pkey PRIMARY KEY (id);


--
-- Name: iot_ota_task iot_ota_task_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_ota_task
    ADD CONSTRAINT iot_ota_task_pkey PRIMARY KEY (id);


--
-- Name: iot_product_category iot_product_category_copy1_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_product_category
    ADD CONSTRAINT iot_product_category_copy1_pkey PRIMARY KEY (id);


--
-- Name: iot_product iot_product_copy1_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_product
    ADD CONSTRAINT iot_product_copy1_pkey PRIMARY KEY (id);


--
-- Name: iot_product_metadata_version iot_product_metadata_version_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_product_metadata_version
    ADD CONSTRAINT iot_product_metadata_version_pkey PRIMARY KEY (id);


--
-- Name: iot_protocol_revision iot_protocol_revision_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_protocol_revision
    ADD CONSTRAINT iot_protocol_revision_pkey PRIMARY KEY (id);


--
-- Name: iot_push_transform_rule iot_push_transform_rule_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_push_transform_rule
    ADD CONSTRAINT iot_push_transform_rule_pkey PRIMARY KEY (id);


--
-- Name: iot_resource_connection iot_resource_connection_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_resource_connection
    ADD CONSTRAINT iot_resource_connection_pkey PRIMARY KEY (id);


--
-- Name: iot_standard_category iot_standard_category_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_standard_category
    ADD CONSTRAINT iot_standard_category_pkey PRIMARY KEY (id);


--
-- Name: iot_standard_field_category_rel iot_standard_field_category_rel_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_standard_field_category_rel
    ADD CONSTRAINT iot_standard_field_category_rel_pkey PRIMARY KEY (id);


--
-- Name: iot_standard_metadata_field iot_standard_metadata_field_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_standard_metadata_field
    ADD CONSTRAINT iot_standard_metadata_field_pkey PRIMARY KEY (id);


--
-- Name: iot_standard_product iot_standard_product_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_standard_product
    ADD CONSTRAINT iot_standard_product_pkey PRIMARY KEY (id);


--
-- Name: iot_sub_account_permission iot_sub_account_permission_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_sub_account_permission
    ADD CONSTRAINT iot_sub_account_permission_pkey PRIMARY KEY (id);


--
-- Name: iot_task_center iot_task_center_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_task_center
    ADD CONSTRAINT iot_task_center_pkey PRIMARY KEY (id);


--
-- Name: iot_task_center iot_task_center_task_no_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_task_center
    ADD CONSTRAINT iot_task_center_task_no_key UNIQUE (task_no);


--
-- Name: iot_task_definition iot_task_definition_definition_no_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_task_definition
    ADD CONSTRAINT iot_task_definition_definition_no_key UNIQUE (definition_no);


--
-- Name: iot_task_definition iot_task_definition_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_task_definition
    ADD CONSTRAINT iot_task_definition_pkey PRIMARY KEY (id);


--
-- Name: iot_task_item iot_task_item_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_task_item
    ADD CONSTRAINT iot_task_item_pkey PRIMARY KEY (id);


--
-- Name: iot_task_log iot_task_log_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_task_log
    ADD CONSTRAINT iot_task_log_pkey PRIMARY KEY (id);


--
-- Name: iot_tenant_package_menu iot_tenant_package_menu_package_id_menu_id_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_tenant_package_menu
    ADD CONSTRAINT iot_tenant_package_menu_package_id_menu_id_key UNIQUE (package_id, menu_id);


--
-- Name: iot_tenant_package_menu iot_tenant_package_menu_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_tenant_package_menu
    ADD CONSTRAINT iot_tenant_package_menu_pkey PRIMARY KEY (id);


--
-- Name: iot_tenant_package iot_tenant_package_package_name_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_tenant_package
    ADD CONSTRAINT iot_tenant_package_package_name_key UNIQUE (package_name);


--
-- Name: iot_tenant_package iot_tenant_package_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_tenant_package
    ADD CONSTRAINT iot_tenant_package_pkey PRIMARY KEY (package_id);


--
-- Name: iot_tenant iot_tenant_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_tenant
    ADD CONSTRAINT iot_tenant_pkey PRIMARY KEY (tenant_id);


--
-- Name: iot_tenant iot_tenant_tenant_code_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_tenant
    ADD CONSTRAINT iot_tenant_tenant_code_key UNIQUE (tenant_code);


--
-- Name: iot_user_application iot_user_application_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_user_application
    ADD CONSTRAINT iot_user_application_pkey PRIMARY KEY (app_unique_id);


--
-- Name: iot_user iot_user_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_user
    ADD CONSTRAINT iot_user_pkey PRIMARY KEY (id);


--
-- Name: iot_user_sub iot_user_sub_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_user_sub
    ADD CONSTRAINT iot_user_sub_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_3d_config iot_visual_3d_config_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_3d_config
    ADD CONSTRAINT iot_visual_3d_config_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_3d iot_visual_3d_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_3d
    ADD CONSTRAINT iot_visual_3d_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_3d_version iot_visual_3d_version_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_3d_version
    ADD CONSTRAINT iot_visual_3d_version_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_ai iot_visual_ai_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_ai
    ADD CONSTRAINT iot_visual_ai_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_assets iot_visual_assets_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_assets
    ADD CONSTRAINT iot_visual_assets_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_banner iot_visual_banner_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_banner
    ADD CONSTRAINT iot_visual_banner_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_category iot_visual_category_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_category
    ADD CONSTRAINT iot_visual_category_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_component iot_visual_component_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_component
    ADD CONSTRAINT iot_visual_component_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_config iot_visual_config_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_config
    ADD CONSTRAINT iot_visual_config_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_db iot_visual_db_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_db
    ADD CONSTRAINT iot_visual_db_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_flow iot_visual_flow_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_flow
    ADD CONSTRAINT iot_visual_flow_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_func iot_visual_func_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_func
    ADD CONSTRAINT iot_visual_func_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_glob iot_visual_glob_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_glob
    ADD CONSTRAINT iot_visual_glob_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_map iot_visual_map_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_map
    ADD CONSTRAINT iot_visual_map_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_model iot_visual_model_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_model
    ADD CONSTRAINT iot_visual_model_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_nav iot_visual_nav_pkey1; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_nav
    ADD CONSTRAINT iot_visual_nav_pkey1 PRIMARY KEY (id);


--
-- Name: iot_visual iot_visual_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual
    ADD CONSTRAINT iot_visual_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_push_channel iot_visual_push_channel_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_push_channel
    ADD CONSTRAINT iot_visual_push_channel_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_push_template iot_visual_push_template_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_push_template
    ADD CONSTRAINT iot_visual_push_template_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_record iot_visual_record_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_record
    ADD CONSTRAINT iot_visual_record_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_report iot_visual_report_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_report
    ADD CONSTRAINT iot_visual_report_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_template_data iot_visual_template_data_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_template_data
    ADD CONSTRAINT iot_visual_template_data_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_template iot_visual_template_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_template
    ADD CONSTRAINT iot_visual_template_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_user iot_visual_user_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_user
    ADD CONSTRAINT iot_visual_user_pkey PRIMARY KEY (id);


--
-- Name: iot_visual_version iot_visual_version_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_visual_version
    ADD CONSTRAINT iot_visual_version_pkey PRIMARY KEY (id);


--
-- Name: iot_viz_dashboard iot_viz_dashboard_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_viz_dashboard
    ADD CONSTRAINT iot_viz_dashboard_pkey PRIMARY KEY (id);


--
-- Name: iot_viz_import_record iot_viz_import_record_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_viz_import_record
    ADD CONSTRAINT iot_viz_import_record_pkey PRIMARY KEY (id);


--
-- Name: iot_viz_widget_bundle iot_viz_widget_bundle_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_viz_widget_bundle
    ADD CONSTRAINT iot_viz_widget_bundle_pkey PRIMARY KEY (id);


--
-- Name: iot_viz_widget_type iot_viz_widget_type_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_viz_widget_type
    ADD CONSTRAINT iot_viz_widget_type_pkey PRIMARY KEY (id);


--
-- Name: mp_app mp_app_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_app
    ADD CONSTRAINT mp_app_pkey PRIMARY KEY (id);


--
-- Name: mp_bind_qrcode mp_bind_qrcode_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_bind_qrcode
    ADD CONSTRAINT mp_bind_qrcode_pkey PRIMARY KEY (id);


--
-- Name: mp_component_library mp_component_library_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_component_library
    ADD CONSTRAINT mp_component_library_pkey PRIMARY KEY (id);


--
-- Name: mp_component_type_mapping mp_component_type_mapping_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_component_type_mapping
    ADD CONSTRAINT mp_component_type_mapping_pkey PRIMARY KEY (id);


--
-- Name: mp_detail_layout mp_detail_layout_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_detail_layout
    ADD CONSTRAINT mp_detail_layout_pkey PRIMARY KEY (id);


--
-- Name: mp_diy_page mp_diy_page_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_diy_page
    ADD CONSTRAINT mp_diy_page_pkey PRIMARY KEY (id);


--
-- Name: mp_diy_template mp_diy_template_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_diy_template
    ADD CONSTRAINT mp_diy_template_pkey PRIMARY KEY (id);


--
-- Name: mp_home_component mp_home_component_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_home_component
    ADD CONSTRAINT mp_home_component_pkey PRIMARY KEY (id);


--
-- Name: mp_home_layout mp_home_layout_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_home_layout
    ADD CONSTRAINT mp_home_layout_pkey PRIMARY KEY (id);


--
-- Name: mp_layout_template mp_layout_template_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_layout_template
    ADD CONSTRAINT mp_layout_template_pkey PRIMARY KEY (id);


--
-- Name: mp_user_device mp_user_device_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_user_device
    ADD CONSTRAINT mp_user_device_pkey PRIMARY KEY (id);


--
-- Name: mp_user_permission mp_user_permission_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_user_permission
    ADD CONSTRAINT mp_user_permission_pkey PRIMARY KEY (id);


--
-- Name: mp_user mp_user_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.mp_user
    ADD CONSTRAINT mp_user_pkey PRIMARY KEY (id);


--
-- Name: nexiot_db_backfill_task nexiot_db_backfill_task_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.nexiot_db_backfill_task
    ADD CONSTRAINT nexiot_db_backfill_task_pkey PRIMARY KEY (task_key);


--
-- Name: notice_channel notice_channel_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.notice_channel
    ADD CONSTRAINT notice_channel_pkey PRIMARY KEY (id);


--
-- Name: notice_send_record notice_send_record_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.notice_send_record
    ADD CONSTRAINT notice_send_record_pkey PRIMARY KEY (id);


--
-- Name: notice_template notice_template_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.notice_template
    ADD CONSTRAINT notice_template_pkey PRIMARY KEY (id);


--
-- Name: notice_wechat_openid notice_wechat_openid_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.notice_wechat_openid
    ADD CONSTRAINT notice_wechat_openid_pkey PRIMARY KEY (id);


--
-- Name: oauth2_access_token oauth2_access_token_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.oauth2_access_token
    ADD CONSTRAINT oauth2_access_token_pkey PRIMARY KEY (id);


--
-- Name: oauth2_refresh_token oauth2_refresh_token_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.oauth2_refresh_token
    ADD CONSTRAINT oauth2_refresh_token_pkey PRIMARY KEY (id);


--
-- Name: oauth_client_details oauth_client_details_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.oauth_client_details
    ADD CONSTRAINT oauth_client_details_pkey PRIMARY KEY (client_id);


--
-- Name: protocol_file_backup protocol_file_backup_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.protocol_file_backup
    ADD CONSTRAINT protocol_file_backup_pkey PRIMARY KEY (id, create_date);


--
-- Name: protocol_file protocol_file_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.protocol_file
    ADD CONSTRAINT protocol_file_pkey PRIMARY KEY (file_path);


--
-- Name: qrtz_blob_triggers qrtz_blob_triggers_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_calendars qrtz_calendars_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_calendars
    ADD CONSTRAINT qrtz_calendars_pkey PRIMARY KEY (sched_name, calendar_name);


--
-- Name: qrtz_cron_triggers qrtz_cron_triggers_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_fired_triggers qrtz_fired_triggers_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_fired_triggers
    ADD CONSTRAINT qrtz_fired_triggers_pkey PRIMARY KEY (sched_name, entry_id);


--
-- Name: qrtz_job_details qrtz_job_details_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_job_details
    ADD CONSTRAINT qrtz_job_details_pkey PRIMARY KEY (sched_name, job_name, job_group);


--
-- Name: qrtz_locks qrtz_locks_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_locks
    ADD CONSTRAINT qrtz_locks_pkey PRIMARY KEY (sched_name, lock_name);


--
-- Name: qrtz_paused_trigger_grps qrtz_paused_trigger_grps_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_paused_trigger_grps
    ADD CONSTRAINT qrtz_paused_trigger_grps_pkey PRIMARY KEY (sched_name, trigger_group);


--
-- Name: qrtz_scheduler_state qrtz_scheduler_state_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_scheduler_state
    ADD CONSTRAINT qrtz_scheduler_state_pkey PRIMARY KEY (sched_name, instance_name);


--
-- Name: qrtz_simple_triggers qrtz_simple_triggers_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_simprop_triggers qrtz_simprop_triggers_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_simprop_triggers
    ADD CONSTRAINT qrtz_simprop_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_triggers qrtz_triggers_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: rule_model_instance rule_model_instance_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.rule_model_instance
    ADD CONSTRAINT rule_model_instance_pkey PRIMARY KEY (id);


--
-- Name: rule_model rule_model_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.rule_model
    ADD CONSTRAINT rule_model_pkey PRIMARY KEY (id);


--
-- Name: rulego_chain_log rulego_chain_log_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.rulego_chain_log
    ADD CONSTRAINT rulego_chain_log_pkey PRIMARY KEY (id);


--
-- Name: rulego_chain rulego_chain_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.rulego_chain
    ADD CONSTRAINT rulego_chain_pkey PRIMARY KEY (id);


--
-- Name: scene_linkage scene_linkage_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.scene_linkage
    ADD CONSTRAINT scene_linkage_pkey PRIMARY KEY (id);


--
-- Name: support_map_areas support_map_areas_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.support_map_areas
    ADD CONSTRAINT support_map_areas_pkey PRIMARY KEY (id);


--
-- Name: sys_config sys_config_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_config
    ADD CONSTRAINT sys_config_pkey PRIMARY KEY (config_id);


--
-- Name: sys_dept sys_dept_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_dept
    ADD CONSTRAINT sys_dept_pkey PRIMARY KEY (dept_id);


--
-- Name: sys_dict_data sys_dict_data_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_dict_data
    ADD CONSTRAINT sys_dict_data_pkey PRIMARY KEY (dict_code);


--
-- Name: sys_dict_type sys_dict_type_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_dict_type
    ADD CONSTRAINT sys_dict_type_pkey PRIMARY KEY (dict_id);


--
-- Name: sys_job_log sys_job_log_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_job_log
    ADD CONSTRAINT sys_job_log_pkey PRIMARY KEY (job_log_id);


--
-- Name: sys_job sys_job_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_job
    ADD CONSTRAINT sys_job_pkey PRIMARY KEY (job_id, job_name, job_group);


--
-- Name: sys_logininfor sys_logininfor_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_logininfor
    ADD CONSTRAINT sys_logininfor_pkey PRIMARY KEY (info_id);


--
-- Name: sys_menu sys_menu_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_menu
    ADD CONSTRAINT sys_menu_pkey PRIMARY KEY (menu_id);


--
-- Name: sys_notice sys_notice_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_notice
    ADD CONSTRAINT sys_notice_pkey PRIMARY KEY (notice_id);


--
-- Name: sys_oper_log sys_oper_log_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_oper_log
    ADD CONSTRAINT sys_oper_log_pkey PRIMARY KEY (oper_id);


--
-- Name: sys_oss_config sys_oss_config_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_oss_config
    ADD CONSTRAINT sys_oss_config_pkey PRIMARY KEY (config_id);


--
-- Name: sys_oss sys_oss_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_oss
    ADD CONSTRAINT sys_oss_pkey PRIMARY KEY (oss_id);


--
-- Name: sys_role_dept sys_role_dept_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_role_dept
    ADD CONSTRAINT sys_role_dept_pkey PRIMARY KEY (uuid);


--
-- Name: sys_role_menu sys_role_menu_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_role_menu
    ADD CONSTRAINT sys_role_menu_pkey PRIMARY KEY (uuid);


--
-- Name: sys_role sys_role_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_role
    ADD CONSTRAINT sys_role_pkey PRIMARY KEY (role_id);


--
-- Name: sys_user_role sys_user_role_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.sys_user_role
    ADD CONSTRAINT sys_user_role_pkey PRIMARY KEY (uuid);


--
-- Name: test_include test_include_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.test_include
    ADD CONSTRAINT test_include_pkey PRIMARY KEY (id);


--
-- Name: tmp_user_tenant_map tmp_user_tenant_map_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.tmp_user_tenant_map
    ADD CONSTRAINT tmp_user_tenant_map_pkey PRIMARY KEY (union_id);


--
-- Name: ts_iot_device_log_0 ts_iot_device_log_0_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_0
    ADD CONSTRAINT ts_iot_device_log_0_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_10 ts_iot_device_log_10_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_10
    ADD CONSTRAINT ts_iot_device_log_10_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_11 ts_iot_device_log_11_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_11
    ADD CONSTRAINT ts_iot_device_log_11_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_12 ts_iot_device_log_12_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_12
    ADD CONSTRAINT ts_iot_device_log_12_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_13 ts_iot_device_log_13_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_13
    ADD CONSTRAINT ts_iot_device_log_13_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_14 ts_iot_device_log_14_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_14
    ADD CONSTRAINT ts_iot_device_log_14_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_15 ts_iot_device_log_15_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_15
    ADD CONSTRAINT ts_iot_device_log_15_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_16 ts_iot_device_log_16_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_16
    ADD CONSTRAINT ts_iot_device_log_16_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_17 ts_iot_device_log_17_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_17
    ADD CONSTRAINT ts_iot_device_log_17_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_18 ts_iot_device_log_18_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_18
    ADD CONSTRAINT ts_iot_device_log_18_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_19 ts_iot_device_log_19_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_19
    ADD CONSTRAINT ts_iot_device_log_19_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_1 ts_iot_device_log_1_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_1
    ADD CONSTRAINT ts_iot_device_log_1_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_20 ts_iot_device_log_20_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_20
    ADD CONSTRAINT ts_iot_device_log_20_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_21 ts_iot_device_log_21_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_21
    ADD CONSTRAINT ts_iot_device_log_21_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_22 ts_iot_device_log_22_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_22
    ADD CONSTRAINT ts_iot_device_log_22_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_23 ts_iot_device_log_23_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_23
    ADD CONSTRAINT ts_iot_device_log_23_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_2 ts_iot_device_log_2_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_2
    ADD CONSTRAINT ts_iot_device_log_2_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_3 ts_iot_device_log_3_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_3
    ADD CONSTRAINT ts_iot_device_log_3_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_4 ts_iot_device_log_4_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_4
    ADD CONSTRAINT ts_iot_device_log_4_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_5 ts_iot_device_log_5_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_5
    ADD CONSTRAINT ts_iot_device_log_5_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_6 ts_iot_device_log_6_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_6
    ADD CONSTRAINT ts_iot_device_log_6_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_7 ts_iot_device_log_7_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_7
    ADD CONSTRAINT ts_iot_device_log_7_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_8 ts_iot_device_log_8_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_8
    ADD CONSTRAINT ts_iot_device_log_8_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_9 ts_iot_device_log_9_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_9
    ADD CONSTRAINT ts_iot_device_log_9_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_0 ts_iot_device_log_metadata_0_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_0
    ADD CONSTRAINT ts_iot_device_log_metadata_0_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_10 ts_iot_device_log_metadata_10_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_10
    ADD CONSTRAINT ts_iot_device_log_metadata_10_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_11 ts_iot_device_log_metadata_11_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_11
    ADD CONSTRAINT ts_iot_device_log_metadata_11_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_12 ts_iot_device_log_metadata_12_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_12
    ADD CONSTRAINT ts_iot_device_log_metadata_12_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_13 ts_iot_device_log_metadata_13_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_13
    ADD CONSTRAINT ts_iot_device_log_metadata_13_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_14 ts_iot_device_log_metadata_14_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_14
    ADD CONSTRAINT ts_iot_device_log_metadata_14_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_15 ts_iot_device_log_metadata_15_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_15
    ADD CONSTRAINT ts_iot_device_log_metadata_15_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_16 ts_iot_device_log_metadata_16_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_16
    ADD CONSTRAINT ts_iot_device_log_metadata_16_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_17 ts_iot_device_log_metadata_17_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_17
    ADD CONSTRAINT ts_iot_device_log_metadata_17_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_18 ts_iot_device_log_metadata_18_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_18
    ADD CONSTRAINT ts_iot_device_log_metadata_18_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_19 ts_iot_device_log_metadata_19_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_19
    ADD CONSTRAINT ts_iot_device_log_metadata_19_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_1 ts_iot_device_log_metadata_1_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_1
    ADD CONSTRAINT ts_iot_device_log_metadata_1_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_20 ts_iot_device_log_metadata_20_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_20
    ADD CONSTRAINT ts_iot_device_log_metadata_20_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_21 ts_iot_device_log_metadata_21_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_21
    ADD CONSTRAINT ts_iot_device_log_metadata_21_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_22 ts_iot_device_log_metadata_22_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_22
    ADD CONSTRAINT ts_iot_device_log_metadata_22_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_23 ts_iot_device_log_metadata_23_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_23
    ADD CONSTRAINT ts_iot_device_log_metadata_23_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_2 ts_iot_device_log_metadata_2_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_2
    ADD CONSTRAINT ts_iot_device_log_metadata_2_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_3 ts_iot_device_log_metadata_3_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_3
    ADD CONSTRAINT ts_iot_device_log_metadata_3_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_4 ts_iot_device_log_metadata_4_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_4
    ADD CONSTRAINT ts_iot_device_log_metadata_4_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_5 ts_iot_device_log_metadata_5_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_5
    ADD CONSTRAINT ts_iot_device_log_metadata_5_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_6 ts_iot_device_log_metadata_6_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_6
    ADD CONSTRAINT ts_iot_device_log_metadata_6_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_7 ts_iot_device_log_metadata_7_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_7
    ADD CONSTRAINT ts_iot_device_log_metadata_7_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_8 ts_iot_device_log_metadata_8_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_8
    ADD CONSTRAINT ts_iot_device_log_metadata_8_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata_9 ts_iot_device_log_metadata_9_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata_9
    ADD CONSTRAINT ts_iot_device_log_metadata_9_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log_metadata ts_iot_device_log_metadata_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log_metadata
    ADD CONSTRAINT ts_iot_device_log_metadata_pkey PRIMARY KEY (id, create_time);


--
-- Name: ts_iot_device_log ts_iot_device_log_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.ts_iot_device_log
    ADD CONSTRAINT ts_iot_device_log_pkey PRIMARY KEY (id, create_time);


--
-- Name: iot_alarm_condition_state uk_alarm_condition_state_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_condition_state
    ADD CONSTRAINT uk_alarm_condition_state_key UNIQUE (tenant_id, correlation_key);


--
-- Name: iot_alarm_evidence uk_alarm_evidence_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_evidence
    ADD CONSTRAINT uk_alarm_evidence_key UNIQUE (tenant_id, correlation_key);


--
-- Name: iot_alarm_inbox uk_alarm_inbox_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_inbox
    ADD CONSTRAINT uk_alarm_inbox_key UNIQUE (tenant_id, correlation_key);


--
-- Name: iot_alarm_key_lock uk_alarm_key_lock_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_key_lock
    ADD CONSTRAINT uk_alarm_key_lock_key UNIQUE (tenant_id, correlation_key);


--
-- Name: iot_alarm_policy uk_alarm_policy_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_policy
    ADD CONSTRAINT uk_alarm_policy_key UNIQUE (tenant_id, correlation_key);


--
-- Name: iot_alarm_record uk_alarm_record_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_record
    ADD CONSTRAINT uk_alarm_record_key UNIQUE (tenant_id, correlation_key);


--
-- Name: iot_alarm_task uk_alarm_task_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_task
    ADD CONSTRAINT uk_alarm_task_key UNIQUE (tenant_id, correlation_key);


--
-- Name: iot_alarm_timeline uk_alarm_timeline_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_timeline
    ADD CONSTRAINT uk_alarm_timeline_key UNIQUE (tenant_id, correlation_key);


--
-- Name: iot_alarm_type uk_alarm_type_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_type
    ADD CONSTRAINT uk_alarm_type_key UNIQUE (tenant_id, correlation_key);


--
-- Name: iot_alarm_work_order uk_alarm_work_order_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_alarm_work_order
    ADD CONSTRAINT uk_alarm_work_order_key UNIQUE (tenant_id, correlation_key);


--
-- Name: iot_industrial_config_import_preview uk_iot_ind_config_import_preview_token; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_config_import_preview
    ADD CONSTRAINT uk_iot_ind_config_import_preview_token UNIQUE (token);


--
-- Name: iot_industrial_config_template uk_iot_ind_config_template_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_config_template
    ADD CONSTRAINT uk_iot_ind_config_template_key UNIQUE (tenant_id, template_key);


--
-- Name: iot_industrial_config_template uk_iot_ind_config_template_name; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_config_template
    ADD CONSTRAINT uk_iot_ind_config_template_name UNIQUE (tenant_id, name);


--
-- Name: iot_industrial_config_template_version uk_iot_ind_config_template_version; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_config_template_version
    ADD CONSTRAINT uk_iot_ind_config_template_version UNIQUE (template_id, version);


--
-- Name: iot_industrial_asset uk_iot_industrial_asset_code; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_asset
    ADD CONSTRAINT uk_iot_industrial_asset_code UNIQUE (tenant_id, endpoint_id, asset_code);


--
-- Name: iot_industrial_collect_group uk_iot_industrial_collect_group; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_collect_group
    ADD CONSTRAINT uk_iot_industrial_collect_group UNIQUE (tenant_id, endpoint_id, group_code);


--
-- Name: iot_industrial_collect_point uk_iot_industrial_collect_point; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_collect_point
    ADD CONSTRAINT uk_iot_industrial_collect_point UNIQUE (group_id, point_id);


--
-- Name: iot_industrial_endpoint uk_iot_industrial_endpoint_key; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_endpoint
    ADD CONSTRAINT uk_iot_industrial_endpoint_key UNIQUE (tenant_id, endpoint_key);


--
-- Name: iot_industrial_point uk_iot_industrial_point_code; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_point
    ADD CONSTRAINT uk_iot_industrial_point_code UNIQUE (tenant_id, endpoint_id, point_code);


--
-- Name: iot_industrial_point uk_iot_industrial_point_property; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_point
    ADD CONSTRAINT uk_iot_industrial_point_property UNIQUE (tenant_id, product_key, device_id, property_key);


--
-- Name: iot_industrial_runtime_stat uk_iot_industrial_runtime_stat; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_runtime_stat
    ADD CONSTRAINT uk_iot_industrial_runtime_stat UNIQUE (endpoint_id, group_id);


--
-- Name: notice_wechat_openid uk_notice_wechat_openid_channel_user; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.notice_wechat_openid
    ADD CONSTRAINT uk_notice_wechat_openid_channel_user UNIQUE (channel_id, user_key);


--
-- Name: video_ai_algorithm_task_log video_ai_algorithm_task_log_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_algorithm_task_log
    ADD CONSTRAINT video_ai_algorithm_task_log_pkey PRIMARY KEY (id);


--
-- Name: video_ai_algorithm_task video_ai_algorithm_task_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_algorithm_task
    ADD CONSTRAINT video_ai_algorithm_task_pkey PRIMARY KEY (id);


--
-- Name: video_ai_algorithm_task_result video_ai_algorithm_task_result_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_algorithm_task_result
    ADD CONSTRAINT video_ai_algorithm_task_result_pkey PRIMARY KEY (id);


--
-- Name: video_ai_annotation_candidate video_ai_annotation_candidate_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_annotation_candidate
    ADD CONSTRAINT video_ai_annotation_candidate_pkey PRIMARY KEY (id);


--
-- Name: video_ai_dataset_artifact video_ai_dataset_artifact_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_dataset_artifact
    ADD CONSTRAINT video_ai_dataset_artifact_pkey PRIMARY KEY (id);


--
-- Name: video_ai_dataset_capture_job video_ai_dataset_capture_job_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_dataset_capture_job
    ADD CONSTRAINT video_ai_dataset_capture_job_pkey PRIMARY KEY (id);


--
-- Name: video_ai_dataset_class video_ai_dataset_class_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_dataset_class
    ADD CONSTRAINT video_ai_dataset_class_pkey PRIMARY KEY (id);


--
-- Name: video_ai_dataset_frame_candidate video_ai_dataset_frame_candidate_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_dataset_frame_candidate
    ADD CONSTRAINT video_ai_dataset_frame_candidate_pkey PRIMARY KEY (id);


--
-- Name: video_ai_dataset_item video_ai_dataset_item_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_dataset_item
    ADD CONSTRAINT video_ai_dataset_item_pkey PRIMARY KEY (id);


--
-- Name: video_ai_dataset video_ai_dataset_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_dataset
    ADD CONSTRAINT video_ai_dataset_pkey PRIMARY KEY (id);


--
-- Name: video_ai_dataset_quality_report video_ai_dataset_quality_report_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_dataset_quality_report
    ADD CONSTRAINT video_ai_dataset_quality_report_pkey PRIMARY KEY (id);


--
-- Name: video_ai_dataset_version video_ai_dataset_version_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_dataset_version
    ADD CONSTRAINT video_ai_dataset_version_pkey PRIMARY KEY (id);


--
-- Name: video_ai_deploy_service video_ai_deploy_service_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_deploy_service
    ADD CONSTRAINT video_ai_deploy_service_pkey PRIMARY KEY (id);


--
-- Name: video_ai_frame_extractor video_ai_frame_extractor_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_frame_extractor
    ADD CONSTRAINT video_ai_frame_extractor_pkey PRIMARY KEY (id);


--
-- Name: video_ai_media_server video_ai_media_server_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_media_server
    ADD CONSTRAINT video_ai_media_server_pkey PRIMARY KEY (id);


--
-- Name: video_ai_model video_ai_model_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_model
    ADD CONSTRAINT video_ai_model_pkey PRIMARY KEY (id);


--
-- Name: video_ai_result_sorter video_ai_result_sorter_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_result_sorter
    ADD CONSTRAINT video_ai_result_sorter_pkey PRIMARY KEY (id);


--
-- Name: video_ai_runtime_node video_ai_runtime_node_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_runtime_node
    ADD CONSTRAINT video_ai_runtime_node_pkey PRIMARY KEY (id);


--
-- Name: video_ai_sample_annotation video_ai_sample_annotation_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_sample_annotation
    ADD CONSTRAINT video_ai_sample_annotation_pkey PRIMARY KEY (id);


--
-- Name: video_ai_task video_ai_task_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_ai_task
    ADD CONSTRAINT video_ai_task_pkey PRIMARY KEY (id);


--
-- Name: video_platform_channel video_platform_channel_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_platform_channel
    ADD CONSTRAINT video_platform_channel_pkey PRIMARY KEY (id);


--
-- Name: video_platform_device_cache video_platform_device_cache_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_platform_device_cache
    ADD CONSTRAINT video_platform_device_cache_pkey PRIMARY KEY (id);


--
-- Name: video_platform_device_ext video_platform_device_ext_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_platform_device_ext
    ADD CONSTRAINT video_platform_device_ext_pkey PRIMARY KEY (id);


--
-- Name: video_platform_device video_platform_device_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_platform_device
    ADD CONSTRAINT video_platform_device_pkey PRIMARY KEY (id);


--
-- Name: video_platform_instance video_platform_instance_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_platform_instance
    ADD CONSTRAINT video_platform_instance_pkey PRIMARY KEY (id);


--
-- Name: video_platform_org_cache video_platform_org_cache_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_platform_org_cache
    ADD CONSTRAINT video_platform_org_cache_pkey PRIMARY KEY (id);


--
-- Name: video_sdk_record_task video_sdk_record_task_pkey; Type: CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_sdk_record_task
    ADD CONSTRAINT video_sdk_record_task_pkey PRIMARY KEY (id);


--
-- Name: app_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX app_id ON unix_tenant_202605.mp_app USING btree (app_id);


--
-- Name: code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX code ON unix_tenant_202605.mp_bind_qrcode USING btree (code);


--
-- Name: dev_function_task_id_uindex; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX dev_function_task_id_uindex ON unix_tenant_202605.iot_device_function_task USING btree (id);


--
-- Name: dev_iot_id_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX dev_iot_id_copy1 ON unix_tenant_202605.iot_device_shadow USING btree (iot_id);


--
-- Name: dev_iot_id_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX dev_iot_id_idx ON unix_tenant_202605.iot_device_tags USING btree (iot_id);


--
-- Name: dev_tag_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX dev_tag_idx ON unix_tenant_202605.iot_device_tags USING btree (tag_key, tag_value);


--
-- Name: dict_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX dict_type ON unix_tenant_202605.sys_dict_type USING btree (dict_type);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON unix_tenant_202605.flyway_schema_history USING btree (success);


--
-- Name: idx_account_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX idx_account_tenant ON unix_tenant_202605.iot_visual_user USING btree (account, tenant_id);


--
-- Name: idx_ai_capability_profile; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ai_capability_profile ON unix_tenant_202605.ai_model_capability USING btree (model_profile_id, deleted, enabled);


--
-- Name: idx_ai_capability_scenario; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ai_capability_scenario ON unix_tenant_202605.ai_model_capability USING btree (scenario_code, capability_code, enabled, deleted);


--
-- Name: idx_ai_profile_provider; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ai_profile_provider ON unix_tenant_202605.ai_model_profile USING btree (provider_id, deleted, status);


--
-- Name: idx_ai_profile_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ai_profile_tenant ON unix_tenant_202605.ai_model_profile USING btree (tenant_id, deleted, status);


--
-- Name: idx_ai_prompt_scenario; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ai_prompt_scenario ON unix_tenant_202605.ai_prompt_template USING btree (scenario_code, status, deleted);


--
-- Name: idx_ai_provider_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ai_provider_tenant ON unix_tenant_202605.ai_model_provider USING btree (tenant_id, deleted, status);


--
-- Name: idx_alarm_condition_state_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_condition_state_list ON unix_tenant_202605.iot_alarm_condition_state USING btree (tenant_id, status, updated_at);


--
-- Name: idx_alarm_condition_state_ref; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_condition_state_ref ON unix_tenant_202605.iot_alarm_condition_state USING btree (tenant_id, reference_id);


--
-- Name: idx_alarm_evidence_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_evidence_list ON unix_tenant_202605.iot_alarm_evidence USING btree (tenant_id, status, updated_at);


--
-- Name: idx_alarm_evidence_ref; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_evidence_ref ON unix_tenant_202605.iot_alarm_evidence USING btree (tenant_id, reference_id);


--
-- Name: idx_alarm_inbox_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_inbox_list ON unix_tenant_202605.iot_alarm_inbox USING btree (tenant_id, status, updated_at);


--
-- Name: idx_alarm_inbox_ref; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_inbox_ref ON unix_tenant_202605.iot_alarm_inbox USING btree (tenant_id, reference_id);


--
-- Name: idx_alarm_key_lock_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_key_lock_list ON unix_tenant_202605.iot_alarm_key_lock USING btree (tenant_id, status, updated_at);


--
-- Name: idx_alarm_key_lock_ref; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_key_lock_ref ON unix_tenant_202605.iot_alarm_key_lock USING btree (tenant_id, reference_id);


--
-- Name: idx_alarm_policy_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_policy_list ON unix_tenant_202605.iot_alarm_policy USING btree (tenant_id, status, updated_at);


--
-- Name: idx_alarm_policy_ref; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_policy_ref ON unix_tenant_202605.iot_alarm_policy USING btree (tenant_id, reference_id);


--
-- Name: idx_alarm_record_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_record_list ON unix_tenant_202605.iot_alarm_record USING btree (tenant_id, status, updated_at);


--
-- Name: idx_alarm_record_ref; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_record_ref ON unix_tenant_202605.iot_alarm_record USING btree (tenant_id, reference_id);


--
-- Name: idx_alarm_task_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_task_list ON unix_tenant_202605.iot_alarm_task USING btree (tenant_id, status, updated_at);


--
-- Name: idx_alarm_task_ref; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_task_ref ON unix_tenant_202605.iot_alarm_task USING btree (tenant_id, reference_id);


--
-- Name: idx_alarm_timeline_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_timeline_list ON unix_tenant_202605.iot_alarm_timeline USING btree (tenant_id, status, updated_at);


--
-- Name: idx_alarm_timeline_ref; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_timeline_ref ON unix_tenant_202605.iot_alarm_timeline USING btree (tenant_id, reference_id);


--
-- Name: idx_alarm_type_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_type_list ON unix_tenant_202605.iot_alarm_type USING btree (tenant_id, status, updated_at);


--
-- Name: idx_alarm_type_ref; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_type_ref ON unix_tenant_202605.iot_alarm_type USING btree (tenant_id, reference_id);


--
-- Name: idx_alarm_work_order_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_work_order_list ON unix_tenant_202605.iot_alarm_work_order USING btree (tenant_id, status, updated_at);


--
-- Name: idx_alarm_work_order_ref; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alarm_work_order_ref ON unix_tenant_202605.iot_alarm_work_order USING btree (tenant_id, reference_id);


--
-- Name: idx_alert_level; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_alert_level ON unix_tenant_202605.iot_algorithm_alert_record USING btree (alert_level, create_time);


--
-- Name: idx_algorithm_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_algorithm_type ON unix_tenant_202605.iot_algorithm_alert_record USING btree (algorithm_type);


--
-- Name: idx_analysis_page_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_analysis_page_tenant ON unix_tenant_202605.iot_analysis_page USING btree (tenant_id, status);


--
-- Name: idx_analysis_template_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_analysis_template_tenant ON unix_tenant_202605.iot_analysis_template USING btree (tenant_id, status);


--
-- Name: idx_app_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX idx_app_id ON unix_tenant_202605.iot_user_application USING btree (app_id);


--
-- Name: idx_app_openid; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_app_openid ON unix_tenant_202605.mp_user USING btree (app_id, open_id);


--
-- Name: idx_app_phone; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_app_phone ON unix_tenant_202605.mp_user USING btree (app_id, phone);


--
-- Name: idx_app_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_app_status ON unix_tenant_202605.mp_bind_qrcode USING btree (app_id, status);


--
-- Name: idx_app_user; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_app_user ON unix_tenant_202605.mp_user_device USING btree (app_id, user_id);


--
-- Name: idx_bridge_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_bridge_type ON unix_tenant_202605.iot_data_bridge_config USING btree (bridge_type);


--
-- Name: idx_camera_index_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_camera_index_code ON unix_tenant_202605.video_platform_channel USING btree (camera_index_code);


--
-- Name: idx_category; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_category ON unix_tenant_202605.mp_home_component USING btree (category);


--
-- Name: idx_category_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_category_id ON unix_tenant_202605.iot_standard_field_category_rel USING btree (category_id);


--
-- Name: idx_channel_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_channel_code ON unix_tenant_202605.video_platform_channel USING btree (channel_code);


--
-- Name: idx_channel_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_channel_status ON unix_tenant_202605.video_platform_channel USING btree (channel_status);


--
-- Name: idx_cid; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_cid ON unix_tenant_202605.iot_device_rule_log USING btree (c_id);


--
-- Name: idx_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_code ON unix_tenant_202605.mp_bind_qrcode USING btree (code);


--
-- Name: idx_component_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_component_type ON unix_tenant_202605.mp_home_component USING btree (component_type);


--
-- Name: idx_config_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_config_id ON unix_tenant_202605.notice_send_record USING btree (config_id);


--
-- Name: idx_create_date; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_create_date ON unix_tenant_202605.iot_device_log USING btree (create_time);


--
-- Name: idx_create_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_create_time ON unix_tenant_202605.iot_product_metadata_version USING btree (create_time);


--
-- Name: idx_creator_id_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_creator_id_copy1 ON unix_tenant_202605.iot_device USING btree (creator_id);


--
-- Name: idx_data_direction; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_data_direction ON unix_tenant_202605.iot_resource_connection USING btree (data_direction);


--
-- Name: idx_deleted; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_deleted ON unix_tenant_202605.mp_diy_page USING btree (deleted);


--
-- Name: idx_device_group_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_device_group_tenant ON unix_tenant_202605.iot_device_group USING btree (tenant_id, create_time);


--
-- Name: idx_device_id_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX idx_device_id_copy1 ON unix_tenant_202605.iot_device USING btree (product_key, device_id);


--
-- Name: idx_device_name_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_device_name_copy1 ON unix_tenant_202605.iot_device USING btree (device_name);


--
-- Name: idx_device_product_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_device_product_key ON unix_tenant_202605.iot_device_log USING btree (device_id, product_key);


--
-- Name: idx_device_sn; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_device_sn ON unix_tenant_202605.video_platform_device_ext USING btree (device_sn);


--
-- Name: idx_device_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_device_status ON unix_tenant_202605.video_platform_device USING btree (device_status);


--
-- Name: idx_device_tenant_dept_online_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_device_tenant_dept_online_copy1 ON unix_tenant_202605.iot_device USING btree (tenant_id, dept_id, state, online_time);


--
-- Name: idx_device_tenant_online_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_device_tenant_online_copy1 ON unix_tenant_202605.iot_device USING btree (tenant_id, product_key, state, online_time);


--
-- Name: idx_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_device_time ON unix_tenant_202605.iot_algorithm_alert_record USING btree (device_id, create_time);


--
-- Name: idx_direction; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_direction ON unix_tenant_202605.iot_resource_connection USING btree (direction);


--
-- Name: idx_enable; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_enable ON unix_tenant_202605.iot_algorithm_config USING btree (enable);


--
-- Name: idx_enabled; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_enabled ON unix_tenant_202605.video_platform_device USING btree (enabled);


--
-- Name: idx_encode_dev_index_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_encode_dev_index_code ON unix_tenant_202605.video_platform_device_ext USING btree (encode_dev_index_code);


--
-- Name: idx_execution_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_execution_id ON unix_tenant_202605.rulego_chain_log USING btree (execution_id);


--
-- Name: idx_expire; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_expire ON unix_tenant_202605.mp_bind_qrcode USING btree (expire_time);


--
-- Name: idx_fence_rel_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_fence_rel_tenant ON unix_tenant_202605.iot_device_fence_rel USING btree (tenant_id, create_date);


--
-- Name: idx_field_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_field_id ON unix_tenant_202605.iot_standard_field_category_rel USING btree (field_id);


--
-- Name: idx_field_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_field_type ON unix_tenant_202605.iot_standard_metadata_field USING btree (field_type);


--
-- Name: idx_flow_definition_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_flow_definition_tenant ON unix_tenant_202605.iot_flow_definition USING btree (tenant_id, deleted, updated_at);


--
-- Name: idx_flow_deployment_enabled; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_flow_deployment_enabled ON unix_tenant_202605.iot_flow_deployment USING btree (enabled, lease_until);


--
-- Name: idx_flow_execution_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_flow_execution_tenant ON unix_tenant_202605.iot_flow_execution USING btree (tenant_id, definition_id, created_at);


--
-- Name: idx_gateway; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_gateway ON unix_tenant_202605.iot_gateway_polling_command USING btree (gateway_product_key, gateway_device_id, execution_order);


--
-- Name: idx_geo_fence_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_geo_fence_tenant ON unix_tenant_202605.iot_device_geo_fence USING btree (tenant_id, create_date);


--
-- Name: idx_instance_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_instance_key ON unix_tenant_202605.video_platform_device USING btree (instance_key);


--
-- Name: idx_iot_ai_document_session; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_document_session ON unix_tenant_202605.iot_ai_document USING btree (session_id);


--
-- Name: idx_iot_ai_draft_session; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_draft_session ON unix_tenant_202605.iot_ai_draft USING btree (session_id);


--
-- Name: idx_iot_ai_draft_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_draft_type ON unix_tenant_202605.iot_ai_draft USING btree (draft_type);


--
-- Name: idx_iot_ai_provider_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_provider_tenant ON unix_tenant_202605.iot_ai_provider USING btree (tenant_id);


--
-- Name: idx_iot_ai_provider_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_provider_type ON unix_tenant_202605.iot_ai_provider USING btree (provider_type);


--
-- Name: idx_iot_ai_resource_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_resource_tenant ON unix_tenant_202605.iot_ai_resource USING btree (tenant_id);


--
-- Name: idx_iot_ai_resource_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_resource_type ON unix_tenant_202605.iot_ai_resource USING btree (resource_type);


--
-- Name: idx_iot_ai_run_log_provider; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_run_log_provider ON unix_tenant_202605.iot_ai_run_log USING btree (provider_id);


--
-- Name: idx_iot_ai_run_log_session; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_run_log_session ON unix_tenant_202605.iot_ai_run_log USING btree (session_id);


--
-- Name: idx_iot_ai_session_product; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_session_product ON unix_tenant_202605.iot_ai_session USING btree (product_key);


--
-- Name: idx_iot_ai_session_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_session_tenant ON unix_tenant_202605.iot_ai_session USING btree (tenant_id);


--
-- Name: idx_iot_ai_skill_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_skill_key ON unix_tenant_202605.iot_ai_skill USING btree (skill_key);


--
-- Name: idx_iot_ai_skill_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ai_skill_tenant ON unix_tenant_202605.iot_ai_skill USING btree (tenant_id);


--
-- Name: idx_iot_analysis_page_menu; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_analysis_page_menu ON unix_tenant_202605.iot_analysis_page USING btree (menu_id);


--
-- Name: idx_iot_analysis_page_template_creator; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_analysis_page_template_creator ON unix_tenant_202605.iot_analysis_page USING btree (template_id, creator_id);


--
-- Name: idx_iot_analysis_template_item_template_sort; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_analysis_template_item_template_sort ON unix_tenant_202605.iot_analysis_template_item USING btree (template_id, sort_no);


--
-- Name: idx_iot_analysis_template_product_creator; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_analysis_template_product_creator ON unix_tenant_202605.iot_analysis_template USING btree (product_key, creator_id);


--
-- Name: idx_iot_certificate_tenant_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_certificate_tenant_type ON unix_tenant_202605.iot_certificate USING btree (tenant_id, certificate_type);


--
-- Name: idx_iot_device_log_0_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_0_tenant_time ON ONLY unix_tenant_202605.iot_device_log_0 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_10_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_10_tenant_time ON ONLY unix_tenant_202605.iot_device_log_10 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_11_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_11_tenant_time ON ONLY unix_tenant_202605.iot_device_log_11 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_12_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_12_tenant_time ON ONLY unix_tenant_202605.iot_device_log_12 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_13_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_13_tenant_time ON ONLY unix_tenant_202605.iot_device_log_13 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_14_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_14_tenant_time ON ONLY unix_tenant_202605.iot_device_log_14 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_15_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_15_tenant_time ON ONLY unix_tenant_202605.iot_device_log_15 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_16_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_16_tenant_time ON ONLY unix_tenant_202605.iot_device_log_16 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_17_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_17_tenant_time ON ONLY unix_tenant_202605.iot_device_log_17 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_18_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_18_tenant_time ON ONLY unix_tenant_202605.iot_device_log_18 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_19_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_19_tenant_time ON ONLY unix_tenant_202605.iot_device_log_19 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_1_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_1_tenant_time ON ONLY unix_tenant_202605.iot_device_log_1 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_20_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_20_tenant_time ON ONLY unix_tenant_202605.iot_device_log_20 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_21_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_21_tenant_time ON ONLY unix_tenant_202605.iot_device_log_21 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_22_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_22_tenant_time ON ONLY unix_tenant_202605.iot_device_log_22 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_23_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_23_tenant_time ON ONLY unix_tenant_202605.iot_device_log_23 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_2_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_2_tenant_time ON ONLY unix_tenant_202605.iot_device_log_2 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_3_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_3_tenant_time ON ONLY unix_tenant_202605.iot_device_log_3 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_4_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_4_tenant_time ON ONLY unix_tenant_202605.iot_device_log_4 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_5_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_5_tenant_time ON ONLY unix_tenant_202605.iot_device_log_5 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_6_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_6_tenant_time ON ONLY unix_tenant_202605.iot_device_log_6 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_7_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_7_tenant_time ON ONLY unix_tenant_202605.iot_device_log_7 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_8_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_8_tenant_time ON ONLY unix_tenant_202605.iot_device_log_8 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_9_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_9_tenant_time ON ONLY unix_tenant_202605.iot_device_log_9 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_id ON unix_tenant_202605.iot_device_log USING btree (id DESC);


--
-- Name: idx_iot_device_log_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_iot_id_time ON unix_tenant_202605.iot_device_log USING btree (iot_id, create_time DESC);


--
-- Name: idx_iot_device_log_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_message_event_time ON unix_tenant_202605.iot_device_log USING btree (message_type, event, create_time DESC);


--
-- Name: idx_iot_device_log_metadata_0_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_0_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_0 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_10_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_10_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_10 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_11_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_11_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_11 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_12_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_12_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_12 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_13_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_13_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_13 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_14_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_14_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_14 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_15_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_15_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_15 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_16_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_16_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_16 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_17_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_17_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_17 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_18_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_18_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_18 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_19_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_19_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_19 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_1_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_1_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_1 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_20_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_20_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_20 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_21_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_21_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_21 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_22_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_22_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_22 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_23_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_23_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_23 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_2_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_2_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_2 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_3_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_3_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_3 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_4_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_4_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_4 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_5_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_5_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_5 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_6_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_6_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_6 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_7_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_7_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_7 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_8_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_8_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_8 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_9_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_9_tenant_time ON ONLY unix_tenant_202605.iot_device_log_metadata_9 USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_metadata_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_id ON unix_tenant_202605.iot_device_log_metadata USING btree (id DESC);


--
-- Name: idx_iot_device_log_metadata_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_iot_event_time ON unix_tenant_202605.iot_device_log_metadata USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_iot_device_log_metadata_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_iot_property_time ON unix_tenant_202605.iot_device_log_metadata USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_iot_device_log_metadata_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_message_time ON unix_tenant_202605.iot_device_log_metadata USING btree (message_type, create_time DESC);


--
-- Name: idx_iot_device_log_metadata_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_metadata_tenant_time ON unix_tenant_202605.iot_device_log_metadata USING btree (tenant_id, create_time);


--
-- Name: idx_iot_device_log_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_product_device_time ON unix_tenant_202605.iot_device_log USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_iot_device_log_tenant_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_device_log_tenant_time ON unix_tenant_202605.iot_device_log USING btree (tenant_id, create_time);


--
-- Name: idx_iot_id_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_id_copy1 ON unix_tenant_202605.iot_device USING btree (iot_id);


--
-- Name: idx_iot_ind_asset_device; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ind_asset_device ON unix_tenant_202605.iot_industrial_asset USING btree (tenant_id, product_key, device_id);


--
-- Name: idx_iot_ind_collect_point_group; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ind_collect_point_group ON unix_tenant_202605.iot_industrial_collect_point USING btree (group_id, enabled, sort_order);


--
-- Name: idx_iot_ind_config_import_preview_owner; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ind_config_import_preview_owner ON unix_tenant_202605.iot_industrial_config_import_preview USING btree (tenant_id, operator_id, status, expire_time);


--
-- Name: idx_iot_ind_config_template_updated; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ind_config_template_updated ON unix_tenant_202605.iot_industrial_config_template USING btree (tenant_id, update_time DESC);


--
-- Name: idx_iot_ind_endpoint_enabled; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ind_endpoint_enabled ON unix_tenant_202605.iot_industrial_endpoint USING btree (tenant_id, enabled, protocol);


--
-- Name: idx_iot_ind_endpoint_owner; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ind_endpoint_owner ON unix_tenant_202605.iot_industrial_endpoint USING btree (tenant_id, owner_product_key, owner_device_id);


--
-- Name: idx_iot_ind_group_endpoint; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ind_group_endpoint ON unix_tenant_202605.iot_industrial_collect_group USING btree (endpoint_id, enabled, collect_mode, interval_ms);


--
-- Name: idx_iot_ind_group_owner; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ind_group_owner ON unix_tenant_202605.iot_industrial_collect_group USING btree (tenant_id, owner_scope, owner_product_key, owner_device_id);


--
-- Name: idx_iot_ind_point_device; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ind_point_device ON unix_tenant_202605.iot_industrial_point USING btree (tenant_id, product_key, device_id);


--
-- Name: idx_iot_ind_point_endpoint; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ind_point_endpoint ON unix_tenant_202605.iot_industrial_point USING btree (endpoint_id, enabled, collect_mode);


--
-- Name: idx_iot_ind_runtime_state; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_ind_runtime_state ON unix_tenant_202605.iot_industrial_runtime_stat USING btree (tenant_id, state, update_time);


--
-- Name: idx_iot_task_center_creator; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_task_center_creator ON unix_tenant_202605.iot_task_center USING btree (creator_id, create_time);


--
-- Name: idx_iot_task_center_definition_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_task_center_definition_status ON unix_tenant_202605.iot_task_center USING btree (definition_id, status);


--
-- Name: idx_iot_task_center_product; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_task_center_product ON unix_tenant_202605.iot_task_center USING btree (product_key, create_time);


--
-- Name: idx_iot_task_center_type_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_task_center_type_status ON unix_tenant_202605.iot_task_center USING btree (task_type, status);


--
-- Name: idx_iot_task_definition_creator; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_task_definition_creator ON unix_tenant_202605.iot_task_definition USING btree (creator_id, create_time);


--
-- Name: idx_iot_task_definition_product; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_task_definition_product ON unix_tenant_202605.iot_task_definition USING btree (product_key, create_time);


--
-- Name: idx_iot_task_definition_schedule; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_task_definition_schedule ON unix_tenant_202605.iot_task_definition USING btree (schedule_type, schedule_status);


--
-- Name: idx_iot_task_item_row; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_task_item_row ON unix_tenant_202605.iot_task_item USING btree (task_id, row_no);


--
-- Name: idx_iot_task_item_task; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_task_item_task ON unix_tenant_202605.iot_task_item USING btree (task_id, status);


--
-- Name: idx_iot_task_log_task; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_task_log_task ON unix_tenant_202605.iot_task_log USING btree (task_id, create_time);


--
-- Name: idx_iot_tenant_package; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_tenant_package ON unix_tenant_202605.iot_tenant USING btree (package_id, status, deleted);


--
-- Name: idx_iot_tenant_package_menu_package; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_tenant_package_menu_package ON unix_tenant_202605.iot_tenant_package_menu USING btree (package_id, menu_id);


--
-- Name: idx_iot_tenant_package_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_tenant_package_status ON unix_tenant_202605.iot_tenant_package USING btree (status, deleted);


--
-- Name: idx_iot_tenant_status_expire; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_tenant_status_expire ON unix_tenant_202605.iot_tenant USING btree (status, expire_time);


--
-- Name: idx_iot_viz_dashboard_tenant_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_viz_dashboard_tenant_status ON unix_tenant_202605.iot_viz_dashboard USING btree (tenant_id, status);


--
-- Name: idx_iot_viz_import_record_dashboard; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_viz_import_record_dashboard ON unix_tenant_202605.iot_viz_import_record USING btree (target_dashboard_id, import_status);


--
-- Name: idx_iot_viz_widget_type_bundle; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_iot_viz_widget_type_bundle ON unix_tenant_202605.iot_viz_widget_type USING btree (bundle_id, status);


--
-- Name: idx_is_anomaly; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_is_anomaly ON unix_tenant_202605.iot_algorithm_result USING btree (is_anomaly);


--
-- Name: idx_is_default; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_is_default ON unix_tenant_202605.mp_home_layout USING btree (is_default);


--
-- Name: idx_level; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_level ON unix_tenant_202605.iot_standard_category USING btree (level);


--
-- Name: idx_log_core_0; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_0 ON ONLY unix_tenant_202605.iot_device_log_0 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_1 ON ONLY unix_tenant_202605.iot_device_log_1 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_10; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_10 ON ONLY unix_tenant_202605.iot_device_log_10 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_11; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_11 ON ONLY unix_tenant_202605.iot_device_log_11 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_12; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_12 ON ONLY unix_tenant_202605.iot_device_log_12 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_13; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_13 ON ONLY unix_tenant_202605.iot_device_log_13 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_14; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_14 ON ONLY unix_tenant_202605.iot_device_log_14 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_15; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_15 ON ONLY unix_tenant_202605.iot_device_log_15 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_16; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_16 ON ONLY unix_tenant_202605.iot_device_log_16 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_17; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_17 ON ONLY unix_tenant_202605.iot_device_log_17 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_18; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_18 ON ONLY unix_tenant_202605.iot_device_log_18 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_19; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_19 ON ONLY unix_tenant_202605.iot_device_log_19 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_2 ON ONLY unix_tenant_202605.iot_device_log_2 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_20; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_20 ON ONLY unix_tenant_202605.iot_device_log_20 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_21; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_21 ON ONLY unix_tenant_202605.iot_device_log_21 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_22; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_22 ON ONLY unix_tenant_202605.iot_device_log_22 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_23; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_23 ON ONLY unix_tenant_202605.iot_device_log_23 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_3 ON ONLY unix_tenant_202605.iot_device_log_3 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_4 ON ONLY unix_tenant_202605.iot_device_log_4 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_5 ON ONLY unix_tenant_202605.iot_device_log_5 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_6 ON ONLY unix_tenant_202605.iot_device_log_6 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_7; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_7 ON ONLY unix_tenant_202605.iot_device_log_7 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_8; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_8 ON ONLY unix_tenant_202605.iot_device_log_8 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_log_core_9; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_log_core_9 ON ONLY unix_tenant_202605.iot_device_log_9 USING btree (iot_id, message_type, create_time, event);


--
-- Name: idx_media_server_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_media_server_id ON unix_tenant_202605.video_platform_device_ext USING btree (media_server_id);


--
-- Name: idx_meta_core_0; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_0 ON ONLY unix_tenant_202605.iot_device_log_metadata_0 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_1 ON ONLY unix_tenant_202605.iot_device_log_metadata_1 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_10; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_10 ON ONLY unix_tenant_202605.iot_device_log_metadata_10 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_11; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_11 ON ONLY unix_tenant_202605.iot_device_log_metadata_11 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_12; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_12 ON ONLY unix_tenant_202605.iot_device_log_metadata_12 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_13; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_13 ON ONLY unix_tenant_202605.iot_device_log_metadata_13 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_14; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_14 ON ONLY unix_tenant_202605.iot_device_log_metadata_14 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_15; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_15 ON ONLY unix_tenant_202605.iot_device_log_metadata_15 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_16; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_16 ON ONLY unix_tenant_202605.iot_device_log_metadata_16 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_17; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_17 ON ONLY unix_tenant_202605.iot_device_log_metadata_17 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_18; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_18 ON ONLY unix_tenant_202605.iot_device_log_metadata_18 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_19; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_19 ON ONLY unix_tenant_202605.iot_device_log_metadata_19 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_2 ON ONLY unix_tenant_202605.iot_device_log_metadata_2 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_20; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_20 ON ONLY unix_tenant_202605.iot_device_log_metadata_20 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_21; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_21 ON ONLY unix_tenant_202605.iot_device_log_metadata_21 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_22; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_22 ON ONLY unix_tenant_202605.iot_device_log_metadata_22 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_23; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_23 ON ONLY unix_tenant_202605.iot_device_log_metadata_23 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_3 ON ONLY unix_tenant_202605.iot_device_log_metadata_3 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_4 ON ONLY unix_tenant_202605.iot_device_log_metadata_4 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_5 ON ONLY unix_tenant_202605.iot_device_log_metadata_5 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_6 ON ONLY unix_tenant_202605.iot_device_log_metadata_6 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_7; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_7 ON ONLY unix_tenant_202605.iot_device_log_metadata_7 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_8; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_8 ON ONLY unix_tenant_202605.iot_device_log_metadata_8 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_meta_core_9; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_meta_core_9 ON ONLY unix_tenant_202605.iot_device_log_metadata_9 USING btree (iot_id, message_type, property, event, id);


--
-- Name: idx_mp_app_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_mp_app_tenant ON unix_tenant_202605.mp_app USING btree (tenant_id, status, deleted);


--
-- Name: idx_mp_user_device_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_mp_user_device_tenant ON unix_tenant_202605.mp_user_device USING btree (tenant_id, user_id);


--
-- Name: idx_mp_user_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_mp_user_tenant ON unix_tenant_202605.mp_user USING btree (tenant_id, status, deleted);


--
-- Name: idx_network_tenant_state; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_network_tenant_state ON unix_tenant_202605.iot_network1 USING btree (tenant_id, type, state);


--
-- Name: idx_network_tenant_state_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_network_tenant_state_copy1 ON unix_tenant_202605.iot_network USING btree (tenant_id, type, state);


--
-- Name: idx_nexiot_backfill_status_updated; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_nexiot_backfill_status_updated ON unix_tenant_202605.nexiot_db_backfill_task USING btree (status, updated_at);


--
-- Name: idx_next_execution_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_next_execution_time ON unix_tenant_202605.iot_data_input_task USING btree (next_execution_time);


--
-- Name: idx_node_red_binding_instance; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_binding_instance ON unix_tenant_202605.iot_node_red_binding USING btree (instance_id, enabled);


--
-- Name: idx_node_red_binding_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_binding_tenant ON unix_tenant_202605.iot_node_red_binding USING btree (tenant_id, product_key);


--
-- Name: idx_node_red_credential_instance; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_credential_instance ON unix_tenant_202605.iot_node_red_credential USING btree (instance_id, status);


--
-- Name: idx_node_red_credential_tenant_edge; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_credential_tenant_edge ON unix_tenant_202605.iot_node_red_credential USING btree (tenant_id, edge_id);


--
-- Name: idx_node_red_deploy_token_instance; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_deploy_token_instance ON unix_tenant_202605.iot_node_red_deployment_token USING btree (instance_id, status);


--
-- Name: idx_node_red_deploy_token_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_deploy_token_tenant ON unix_tenant_202605.iot_node_red_deployment_token USING btree (tenant_id, status, expires_at);


--
-- Name: idx_node_red_flow_deployment_backup; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_flow_deployment_backup ON unix_tenant_202605.iot_node_red_flow_deployment USING btree (instance_id, backup_id);


--
-- Name: idx_node_red_flow_deployment_instance; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_flow_deployment_instance ON unix_tenant_202605.iot_node_red_flow_deployment USING btree (instance_id, created_at);


--
-- Name: idx_node_red_flow_deployment_tenant_edge; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_flow_deployment_tenant_edge ON unix_tenant_202605.iot_node_red_flow_deployment USING btree (tenant_id, edge_id, created_at);


--
-- Name: idx_node_red_flow_snapshot_backup; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_flow_snapshot_backup ON unix_tenant_202605.iot_node_red_flow_snapshot USING btree (instance_id, backup_id);


--
-- Name: idx_node_red_flow_snapshot_instance; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_flow_snapshot_instance ON unix_tenant_202605.iot_node_red_flow_snapshot USING btree (instance_id, created_at);


--
-- Name: idx_node_red_flow_snapshot_tenant_edge; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_flow_snapshot_tenant_edge ON unix_tenant_202605.iot_node_red_flow_snapshot USING btree (tenant_id, edge_id, created_at);


--
-- Name: idx_node_red_tenant_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_node_red_tenant_status ON unix_tenant_202605.iot_node_red_instance USING btree (tenant_id, status, updated_at);


--
-- Name: idx_notice_channel_scope; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_notice_channel_scope ON unix_tenant_202605.notice_channel USING btree (tenant_id, dept_id, create_time);


--
-- Name: idx_notice_send_record_scope; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_notice_send_record_scope ON unix_tenant_202605.notice_send_record USING btree (tenant_id, dept_id, send_time);


--
-- Name: idx_notice_template_scope; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_notice_template_scope ON unix_tenant_202605.notice_template USING btree (tenant_id, dept_id, create_time);


--
-- Name: idx_oat_user_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_oat_user_id ON unix_tenant_202605.oauth2_access_token USING btree (user_id);


--
-- Name: idx_oauth_client_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_oauth_client_tenant ON unix_tenant_202605.oauth_client_details USING btree (tenant_id, client_id);


--
-- Name: idx_online_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_online_copy1 ON unix_tenant_202605.iot_device USING btree (product_key, state, online_time);


--
-- Name: idx_org_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_org_id ON unix_tenant_202605.video_platform_device USING btree (org_id);


--
-- Name: idx_ota_device_version_product; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ota_device_version_product ON unix_tenant_202605.iot_ota_device_version USING btree (product_key, package_type, module);


--
-- Name: idx_ota_package_product; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ota_package_product ON unix_tenant_202605.iot_ota_package USING btree (product_key, package_type, status);


--
-- Name: idx_ota_package_tag_rel_tag; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ota_package_tag_rel_tag ON unix_tenant_202605.iot_ota_package_tag_rel USING btree (tag_id);


--
-- Name: idx_ota_package_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ota_package_tenant ON unix_tenant_202605.iot_ota_package USING btree (tenant_id, product_key, status);


--
-- Name: idx_ota_package_version; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ota_package_version ON unix_tenant_202605.iot_ota_package USING btree (product_key, version);


--
-- Name: idx_ota_tag_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ota_tag_tenant ON unix_tenant_202605.iot_ota_tag USING btree (tenant_id, tag_code);


--
-- Name: idx_ota_task_device_lookup; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ota_task_device_lookup ON unix_tenant_202605.iot_ota_task_device USING btree (product_key, device_id, status);


--
-- Name: idx_ota_task_device_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ota_task_device_status ON unix_tenant_202605.iot_ota_task_device USING btree (task_id, status);


--
-- Name: idx_ota_task_package; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ota_task_package ON unix_tenant_202605.iot_ota_task USING btree (package_id);


--
-- Name: idx_ota_task_product; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ota_task_product ON unix_tenant_202605.iot_ota_task USING btree (product_key, status);


--
-- Name: idx_ota_task_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ota_task_tenant ON unix_tenant_202605.iot_ota_task USING btree (tenant_id, product_key, status);


--
-- Name: idx_page_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_page_type ON unix_tenant_202605.mp_diy_page USING btree (page_type);


--
-- Name: idx_parent_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_parent_id ON unix_tenant_202605.iot_standard_category USING btree (parent_id);


--
-- Name: idx_parent_org_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_parent_org_id ON unix_tenant_202605.video_platform_org_cache USING btree (parent_org_id);


--
-- Name: idx_pk_did_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_pk_did_copy1 ON unix_tenant_202605.iot_device_shadow USING btree (product_key, device_id);


--
-- Name: idx_platform_device_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_platform_device_id ON unix_tenant_202605.video_platform_channel USING btree (platform_device_id);


--
-- Name: idx_platform_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_platform_type ON unix_tenant_202605.video_platform_instance USING btree (platform_type);


--
-- Name: idx_plugin_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_plugin_type ON unix_tenant_202605.iot_resource_connection USING btree (plugin_type);


--
-- Name: idx_polling_due; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_polling_due ON unix_tenant_202605.iot_gateway_polling_config USING btree (task_type, interval_seconds, next_poll_time, polling_status, enabled);


--
-- Name: idx_polling_tenant_due; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_polling_tenant_due ON unix_tenant_202605.iot_gateway_polling_config USING btree (tenant_id, interval_seconds, next_poll_time, polling_status);


--
-- Name: idx_prod_class_id_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_prod_class_id_copy1 ON unix_tenant_202605.iot_product USING btree (classified_id);


--
-- Name: idx_product_category_tenant_parent_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_product_category_tenant_parent_copy1 ON unix_tenant_202605.iot_product_category USING btree (tenant_id, parent_id, has_child);


--
-- Name: idx_product_creator_classified_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_product_creator_classified_copy1 ON unix_tenant_202605.iot_product USING btree (creator_id, classified_id, company_no);


--
-- Name: idx_product_creator_platform_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_product_creator_platform_copy1 ON unix_tenant_202605.iot_product USING btree (creator_id, third_platform, device_node);


--
-- Name: idx_product_creator_search_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_product_creator_search_copy1 ON unix_tenant_202605.iot_product USING btree (creator_id, product_key, name);


--
-- Name: idx_product_creator_state_time_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_product_creator_state_time_copy1 ON unix_tenant_202605.iot_product USING btree (creator_id, state, create_time DESC);


--
-- Name: idx_product_id_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_product_id_copy1 ON unix_tenant_202605.iot_product USING btree (product_id);


--
-- Name: idx_product_key_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX idx_product_key_copy1 ON unix_tenant_202605.iot_product USING btree (product_key);


--
-- Name: idx_product_tenant_state_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_product_tenant_state_copy1 ON unix_tenant_202605.iot_product USING btree (tenant_id, state, create_time);


--
-- Name: idx_product_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_product_time ON unix_tenant_202605.iot_algorithm_result USING btree (product_key, create_time);


--
-- Name: idx_protocol_revision_history; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_protocol_revision_history ON unix_tenant_202605.iot_protocol_revision USING btree (protocol_id, created_at);


--
-- Name: idx_ptr_app_scope_pk; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ptr_app_scope_pk ON unix_tenant_202605.iot_push_transform_rule USING btree (app_unique_id, scope_type, product_key, status, deleted);


--
-- Name: idx_ptr_app_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ptr_app_status ON unix_tenant_202605.iot_push_transform_rule USING btree (app_unique_id, status, deleted);


--
-- Name: idx_ptr_preset_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ptr_preset_code ON unix_tenant_202605.iot_push_transform_rule USING btree (preset_code);


--
-- Name: idx_ptz_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ptz_type ON unix_tenant_202605.video_platform_channel USING btree (ptz_type);


--
-- Name: idx_qrtz_ft_inst_job_req_rcvry; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_ft_inst_job_req_rcvry ON unix_tenant_202605.qrtz_fired_triggers USING btree (sched_name, instance_name, requests_recovery);


--
-- Name: idx_qrtz_ft_j_g; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_ft_j_g ON unix_tenant_202605.qrtz_fired_triggers USING btree (sched_name, job_name, job_group);


--
-- Name: idx_qrtz_ft_jg; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_ft_jg ON unix_tenant_202605.qrtz_fired_triggers USING btree (sched_name, job_group);


--
-- Name: idx_qrtz_ft_t_g; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_ft_t_g ON unix_tenant_202605.qrtz_fired_triggers USING btree (sched_name, trigger_name, trigger_group);


--
-- Name: idx_qrtz_ft_tg; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_ft_tg ON unix_tenant_202605.qrtz_fired_triggers USING btree (sched_name, trigger_group);


--
-- Name: idx_qrtz_ft_trig_inst_name; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_ft_trig_inst_name ON unix_tenant_202605.qrtz_fired_triggers USING btree (sched_name, instance_name);


--
-- Name: idx_qrtz_j_grp; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_j_grp ON unix_tenant_202605.qrtz_job_details USING btree (sched_name, job_group);


--
-- Name: idx_qrtz_j_req_recovery; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_j_req_recovery ON unix_tenant_202605.qrtz_job_details USING btree (sched_name, requests_recovery);


--
-- Name: idx_qrtz_t_c; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_t_c ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, calendar_name);


--
-- Name: idx_qrtz_t_g; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_t_g ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, trigger_group);


--
-- Name: idx_qrtz_t_j; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_t_j ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, job_name, job_group);


--
-- Name: idx_qrtz_t_jg; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_t_jg ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, job_group);


--
-- Name: idx_qrtz_t_n_g_state; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_t_n_g_state ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, trigger_group, trigger_state);


--
-- Name: idx_qrtz_t_n_state; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_t_n_state ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, trigger_name, trigger_group, trigger_state);


--
-- Name: idx_qrtz_t_next_fire_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_t_next_fire_time ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, next_fire_time);


--
-- Name: idx_qrtz_t_nft_misfire; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_t_nft_misfire ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, misfire_instr, next_fire_time);


--
-- Name: idx_qrtz_t_nft_st; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_t_nft_st ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, trigger_state, next_fire_time);


--
-- Name: idx_qrtz_t_nft_st_misfire; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_t_nft_st_misfire ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, misfire_instr, next_fire_time, trigger_state);


--
-- Name: idx_qrtz_t_nft_st_misfire_grp; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_t_nft_st_misfire_grp ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, misfire_instr, next_fire_time, trigger_group, trigger_state);


--
-- Name: idx_qrtz_t_state; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_qrtz_t_state ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, trigger_state);


--
-- Name: idx_rule_model_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_rule_model_tenant ON unix_tenant_202605.rule_model USING btree (tenant_id, product_key, status);


--
-- Name: idx_rulego_chain_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_rulego_chain_tenant ON unix_tenant_202605.rulego_chain USING btree (tenant_id, status, create_time);


--
-- Name: idx_rulego_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_rulego_id ON unix_tenant_202605.rulego_chain_log USING btree (rulego_id);


--
-- Name: idx_scene_linkage_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_scene_linkage_tenant ON unix_tenant_202605.scene_linkage USING btree (tenant_id, status, create_time);


--
-- Name: idx_sdk_record_task_due; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_sdk_record_task_due ON unix_tenant_202605.video_sdk_record_task USING btree (next_poll_at, lease_until, status);


--
-- Name: idx_sdk_record_task_scope; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_sdk_record_task_scope ON unix_tenant_202605.video_sdk_record_task USING btree (tenant_id, instance_key, created_at);


--
-- Name: idx_slave; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_slave ON unix_tenant_202605.iot_gateway_polling_command USING btree (slave_device_id);


--
-- Name: idx_source_scope; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_source_scope ON unix_tenant_202605.iot_data_bridge_config USING btree (source_scope);


--
-- Name: idx_standard_category_tenant_parent; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_standard_category_tenant_parent ON unix_tenant_202605.iot_standard_category USING btree (tenant_id, parent_id, level);


--
-- Name: idx_standard_field_tenant_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_standard_field_tenant_type ON unix_tenant_202605.iot_standard_metadata_field USING btree (tenant_id, field_type, status);


--
-- Name: idx_standard_product_tenant_category; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_standard_product_tenant_category ON unix_tenant_202605.iot_standard_product USING btree (tenant_id, category_id, status);


--
-- Name: idx_stat_date; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_stat_date ON unix_tenant_202605.iot_device_statistics USING btree (stat_date);


--
-- Name: idx_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_status ON unix_tenant_202605.rulego_chain USING btree (status);


--
-- Name: idx_sub_account_permission_type_res; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_sub_account_permission_type_res ON unix_tenant_202605.iot_sub_account_permission USING btree (resource_type, resource_id);


--
-- Name: idx_sys_dept_tenant_parent; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_sys_dept_tenant_parent ON unix_tenant_202605.sys_dept USING btree (tenant_id, parent_id, status);


--
-- Name: idx_sys_dict_data_type_status_sort; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_sys_dict_data_type_status_sort ON unix_tenant_202605.sys_dict_data USING btree (dict_type, status, dict_sort);


--
-- Name: idx_sys_oss_config_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_sys_oss_config_tenant ON unix_tenant_202605.sys_oss_config USING btree (tenant_id);


--
-- Name: idx_sys_oss_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_sys_oss_tenant ON unix_tenant_202605.sys_oss USING btree (tenant_id);


--
-- Name: idx_sys_role_create_by; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_sys_role_create_by ON unix_tenant_202605.sys_role USING btree (create_by);


--
-- Name: idx_sys_role_dept_role; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_sys_role_dept_role ON unix_tenant_202605.sys_role_dept USING btree (role_id, dept_id);


--
-- Name: idx_sys_role_tenant_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_sys_role_tenant_key ON unix_tenant_202605.sys_role USING btree (tenant_id, role_key);


--
-- Name: idx_sys_role_tenant_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_sys_role_tenant_type ON unix_tenant_202605.sys_role USING btree (tenant_id, role_type, status);


--
-- Name: idx_target_resource_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_target_resource_id ON unix_tenant_202605.iot_data_bridge_config USING btree (target_resource_id);


--
-- Name: idx_task_center_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_task_center_tenant ON unix_tenant_202605.iot_task_center USING btree (tenant_id, task_type, status);


--
-- Name: idx_task_definition_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_task_definition_tenant ON unix_tenant_202605.iot_task_definition USING btree (tenant_id, task_type, schedule_status);


--
-- Name: idx_task_item_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_task_item_tenant ON unix_tenant_202605.iot_task_item USING btree (tenant_id, task_id, status);


--
-- Name: idx_template_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_template_id ON unix_tenant_202605.mp_diy_page USING btree (template_id);


--
-- Name: idx_template_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_template_status ON unix_tenant_202605.iot_node_red_flow_template USING btree (status, deleted);


--
-- Name: idx_template_tenant_category; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_template_tenant_category ON unix_tenant_202605.iot_node_red_flow_template USING btree (tenant_id, category_code, scene_code);


--
-- Name: idx_ts_iot_device_log_0_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_0_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_0 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_0_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_0_id ON unix_tenant_202605.ts_iot_device_log_0 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_0_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_0_iot_id_time ON unix_tenant_202605.ts_iot_device_log_0 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_0_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_0_message_event_time ON unix_tenant_202605.ts_iot_device_log_0 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_0_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_0_product_device_time ON unix_tenant_202605.ts_iot_device_log_0 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_10_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_10_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_10 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_10_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_10_id ON unix_tenant_202605.ts_iot_device_log_10 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_10_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_10_iot_id_time ON unix_tenant_202605.ts_iot_device_log_10 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_10_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_10_message_event_time ON unix_tenant_202605.ts_iot_device_log_10 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_10_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_10_product_device_time ON unix_tenant_202605.ts_iot_device_log_10 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_11_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_11_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_11 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_11_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_11_id ON unix_tenant_202605.ts_iot_device_log_11 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_11_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_11_iot_id_time ON unix_tenant_202605.ts_iot_device_log_11 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_11_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_11_message_event_time ON unix_tenant_202605.ts_iot_device_log_11 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_11_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_11_product_device_time ON unix_tenant_202605.ts_iot_device_log_11 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_12_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_12_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_12 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_12_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_12_id ON unix_tenant_202605.ts_iot_device_log_12 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_12_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_12_iot_id_time ON unix_tenant_202605.ts_iot_device_log_12 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_12_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_12_message_event_time ON unix_tenant_202605.ts_iot_device_log_12 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_12_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_12_product_device_time ON unix_tenant_202605.ts_iot_device_log_12 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_13_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_13_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_13 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_13_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_13_id ON unix_tenant_202605.ts_iot_device_log_13 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_13_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_13_iot_id_time ON unix_tenant_202605.ts_iot_device_log_13 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_13_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_13_message_event_time ON unix_tenant_202605.ts_iot_device_log_13 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_13_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_13_product_device_time ON unix_tenant_202605.ts_iot_device_log_13 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_14_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_14_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_14 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_14_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_14_id ON unix_tenant_202605.ts_iot_device_log_14 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_14_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_14_iot_id_time ON unix_tenant_202605.ts_iot_device_log_14 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_14_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_14_message_event_time ON unix_tenant_202605.ts_iot_device_log_14 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_14_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_14_product_device_time ON unix_tenant_202605.ts_iot_device_log_14 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_15_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_15_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_15 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_15_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_15_id ON unix_tenant_202605.ts_iot_device_log_15 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_15_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_15_iot_id_time ON unix_tenant_202605.ts_iot_device_log_15 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_15_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_15_message_event_time ON unix_tenant_202605.ts_iot_device_log_15 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_15_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_15_product_device_time ON unix_tenant_202605.ts_iot_device_log_15 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_16_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_16_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_16 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_16_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_16_id ON unix_tenant_202605.ts_iot_device_log_16 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_16_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_16_iot_id_time ON unix_tenant_202605.ts_iot_device_log_16 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_16_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_16_message_event_time ON unix_tenant_202605.ts_iot_device_log_16 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_16_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_16_product_device_time ON unix_tenant_202605.ts_iot_device_log_16 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_17_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_17_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_17 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_17_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_17_id ON unix_tenant_202605.ts_iot_device_log_17 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_17_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_17_iot_id_time ON unix_tenant_202605.ts_iot_device_log_17 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_17_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_17_message_event_time ON unix_tenant_202605.ts_iot_device_log_17 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_17_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_17_product_device_time ON unix_tenant_202605.ts_iot_device_log_17 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_18_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_18_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_18 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_18_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_18_id ON unix_tenant_202605.ts_iot_device_log_18 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_18_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_18_iot_id_time ON unix_tenant_202605.ts_iot_device_log_18 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_18_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_18_message_event_time ON unix_tenant_202605.ts_iot_device_log_18 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_18_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_18_product_device_time ON unix_tenant_202605.ts_iot_device_log_18 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_19_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_19_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_19 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_19_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_19_id ON unix_tenant_202605.ts_iot_device_log_19 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_19_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_19_iot_id_time ON unix_tenant_202605.ts_iot_device_log_19 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_19_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_19_message_event_time ON unix_tenant_202605.ts_iot_device_log_19 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_19_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_19_product_device_time ON unix_tenant_202605.ts_iot_device_log_19 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_1_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_1_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_1 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_1_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_1_id ON unix_tenant_202605.ts_iot_device_log_1 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_1_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_1_iot_id_time ON unix_tenant_202605.ts_iot_device_log_1 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_1_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_1_message_event_time ON unix_tenant_202605.ts_iot_device_log_1 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_1_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_1_product_device_time ON unix_tenant_202605.ts_iot_device_log_1 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_20_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_20_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_20 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_20_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_20_id ON unix_tenant_202605.ts_iot_device_log_20 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_20_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_20_iot_id_time ON unix_tenant_202605.ts_iot_device_log_20 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_20_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_20_message_event_time ON unix_tenant_202605.ts_iot_device_log_20 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_20_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_20_product_device_time ON unix_tenant_202605.ts_iot_device_log_20 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_21_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_21_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_21 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_21_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_21_id ON unix_tenant_202605.ts_iot_device_log_21 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_21_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_21_iot_id_time ON unix_tenant_202605.ts_iot_device_log_21 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_21_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_21_message_event_time ON unix_tenant_202605.ts_iot_device_log_21 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_21_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_21_product_device_time ON unix_tenant_202605.ts_iot_device_log_21 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_22_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_22_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_22 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_22_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_22_id ON unix_tenant_202605.ts_iot_device_log_22 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_22_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_22_iot_id_time ON unix_tenant_202605.ts_iot_device_log_22 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_22_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_22_message_event_time ON unix_tenant_202605.ts_iot_device_log_22 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_22_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_22_product_device_time ON unix_tenant_202605.ts_iot_device_log_22 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_23_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_23_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_23 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_23_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_23_id ON unix_tenant_202605.ts_iot_device_log_23 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_23_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_23_iot_id_time ON unix_tenant_202605.ts_iot_device_log_23 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_23_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_23_message_event_time ON unix_tenant_202605.ts_iot_device_log_23 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_23_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_23_product_device_time ON unix_tenant_202605.ts_iot_device_log_23 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_2_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_2_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_2 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_2_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_2_id ON unix_tenant_202605.ts_iot_device_log_2 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_2_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_2_iot_id_time ON unix_tenant_202605.ts_iot_device_log_2 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_2_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_2_message_event_time ON unix_tenant_202605.ts_iot_device_log_2 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_2_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_2_product_device_time ON unix_tenant_202605.ts_iot_device_log_2 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_3_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_3_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_3 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_3_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_3_id ON unix_tenant_202605.ts_iot_device_log_3 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_3_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_3_iot_id_time ON unix_tenant_202605.ts_iot_device_log_3 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_3_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_3_message_event_time ON unix_tenant_202605.ts_iot_device_log_3 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_3_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_3_product_device_time ON unix_tenant_202605.ts_iot_device_log_3 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_4_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_4_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_4 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_4_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_4_id ON unix_tenant_202605.ts_iot_device_log_4 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_4_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_4_iot_id_time ON unix_tenant_202605.ts_iot_device_log_4 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_4_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_4_message_event_time ON unix_tenant_202605.ts_iot_device_log_4 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_4_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_4_product_device_time ON unix_tenant_202605.ts_iot_device_log_4 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_5_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_5_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_5 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_5_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_5_id ON unix_tenant_202605.ts_iot_device_log_5 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_5_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_5_iot_id_time ON unix_tenant_202605.ts_iot_device_log_5 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_5_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_5_message_event_time ON unix_tenant_202605.ts_iot_device_log_5 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_5_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_5_product_device_time ON unix_tenant_202605.ts_iot_device_log_5 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_6_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_6_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_6 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_6_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_6_id ON unix_tenant_202605.ts_iot_device_log_6 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_6_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_6_iot_id_time ON unix_tenant_202605.ts_iot_device_log_6 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_6_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_6_message_event_time ON unix_tenant_202605.ts_iot_device_log_6 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_6_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_6_product_device_time ON unix_tenant_202605.ts_iot_device_log_6 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_7_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_7_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_7 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_7_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_7_id ON unix_tenant_202605.ts_iot_device_log_7 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_7_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_7_iot_id_time ON unix_tenant_202605.ts_iot_device_log_7 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_7_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_7_message_event_time ON unix_tenant_202605.ts_iot_device_log_7 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_7_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_7_product_device_time ON unix_tenant_202605.ts_iot_device_log_7 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_8_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_8_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_8 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_8_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_8_id ON unix_tenant_202605.ts_iot_device_log_8 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_8_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_8_iot_id_time ON unix_tenant_202605.ts_iot_device_log_8 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_8_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_8_message_event_time ON unix_tenant_202605.ts_iot_device_log_8 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_8_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_8_product_device_time ON unix_tenant_202605.ts_iot_device_log_8 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_9_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_9_history_device_v2 ON unix_tenant_202605.ts_iot_device_log_9 USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_9_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_9_id ON unix_tenant_202605.ts_iot_device_log_9 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_9_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_9_iot_id_time ON unix_tenant_202605.ts_iot_device_log_9 USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_9_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_9_message_event_time ON unix_tenant_202605.ts_iot_device_log_9 USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_9_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_9_product_device_time ON unix_tenant_202605.ts_iot_device_log_9 USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_history_device_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_history_device_v2 ON unix_tenant_202605.ts_iot_device_log USING btree (iot_id, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_id ON unix_tenant_202605.ts_iot_device_log USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_iot_id_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_iot_id_time ON unix_tenant_202605.ts_iot_device_log USING btree (iot_id, create_time DESC);


--
-- Name: idx_ts_iot_device_log_message_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_message_event_time ON unix_tenant_202605.ts_iot_device_log USING btree (message_type, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_0_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_0_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_0 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_0_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_0_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_0 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_0_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_0_id ON unix_tenant_202605.ts_iot_device_log_metadata_0 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_0_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_0_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_0 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_0_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_0_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_0 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_0_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_0_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_0 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_10_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_10_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_10 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_10_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_10_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_10 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_10_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_10_id ON unix_tenant_202605.ts_iot_device_log_metadata_10 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_10_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_10_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_10 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_10_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_10_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_10 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_10_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_10_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_10 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_11_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_11_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_11 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_11_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_11_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_11 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_11_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_11_id ON unix_tenant_202605.ts_iot_device_log_metadata_11 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_11_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_11_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_11 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_11_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_11_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_11 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_11_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_11_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_11 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_12_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_12_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_12 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_12_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_12_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_12 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_12_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_12_id ON unix_tenant_202605.ts_iot_device_log_metadata_12 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_12_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_12_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_12 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_12_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_12_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_12 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_12_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_12_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_12 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_13_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_13_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_13 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_13_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_13_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_13 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_13_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_13_id ON unix_tenant_202605.ts_iot_device_log_metadata_13 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_13_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_13_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_13 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_13_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_13_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_13 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_13_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_13_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_13 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_14_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_14_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_14 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_14_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_14_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_14 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_14_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_14_id ON unix_tenant_202605.ts_iot_device_log_metadata_14 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_14_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_14_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_14 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_14_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_14_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_14 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_14_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_14_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_14 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_15_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_15_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_15 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_15_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_15_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_15 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_15_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_15_id ON unix_tenant_202605.ts_iot_device_log_metadata_15 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_15_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_15_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_15 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_15_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_15_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_15 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_15_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_15_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_15 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_16_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_16_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_16 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_16_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_16_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_16 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_16_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_16_id ON unix_tenant_202605.ts_iot_device_log_metadata_16 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_16_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_16_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_16 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_16_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_16_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_16 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_16_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_16_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_16 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_17_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_17_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_17 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_17_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_17_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_17 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_17_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_17_id ON unix_tenant_202605.ts_iot_device_log_metadata_17 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_17_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_17_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_17 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_17_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_17_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_17 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_17_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_17_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_17 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_18_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_18_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_18 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_18_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_18_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_18 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_18_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_18_id ON unix_tenant_202605.ts_iot_device_log_metadata_18 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_18_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_18_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_18 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_18_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_18_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_18 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_18_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_18_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_18 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_19_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_19_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_19 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_19_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_19_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_19 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_19_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_19_id ON unix_tenant_202605.ts_iot_device_log_metadata_19 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_19_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_19_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_19 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_19_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_19_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_19 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_19_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_19_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_19 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_1_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_1_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_1 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_1_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_1_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_1 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_1_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_1_id ON unix_tenant_202605.ts_iot_device_log_metadata_1 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_1_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_1_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_1 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_1_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_1_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_1 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_1_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_1_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_1 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_20_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_20_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_20 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_20_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_20_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_20 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_20_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_20_id ON unix_tenant_202605.ts_iot_device_log_metadata_20 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_20_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_20_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_20 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_20_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_20_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_20 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_20_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_20_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_20 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_21_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_21_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_21 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_21_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_21_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_21 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_21_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_21_id ON unix_tenant_202605.ts_iot_device_log_metadata_21 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_21_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_21_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_21 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_21_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_21_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_21 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_21_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_21_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_21 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_22_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_22_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_22 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_22_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_22_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_22 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_22_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_22_id ON unix_tenant_202605.ts_iot_device_log_metadata_22 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_22_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_22_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_22 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_22_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_22_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_22 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_22_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_22_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_22 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_23_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_23_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_23 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_23_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_23_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_23 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_23_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_23_id ON unix_tenant_202605.ts_iot_device_log_metadata_23 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_23_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_23_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_23 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_23_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_23_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_23 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_23_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_23_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_23 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_2_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_2_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_2 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_2_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_2_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_2 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_2_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_2_id ON unix_tenant_202605.ts_iot_device_log_metadata_2 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_2_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_2_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_2 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_2_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_2_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_2 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_2_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_2_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_2 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_3_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_3_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_3 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_3_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_3_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_3 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_3_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_3_id ON unix_tenant_202605.ts_iot_device_log_metadata_3 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_3_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_3_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_3 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_3_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_3_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_3 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_3_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_3_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_3 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_4_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_4_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_4 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_4_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_4_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_4 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_4_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_4_id ON unix_tenant_202605.ts_iot_device_log_metadata_4 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_4_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_4_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_4 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_4_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_4_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_4 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_4_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_4_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_4 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_5_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_5_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_5 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_5_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_5_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_5 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_5_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_5_id ON unix_tenant_202605.ts_iot_device_log_metadata_5 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_5_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_5_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_5 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_5_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_5_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_5 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_5_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_5_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_5 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_6_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_6_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_6 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_6_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_6_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_6 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_6_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_6_id ON unix_tenant_202605.ts_iot_device_log_metadata_6 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_6_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_6_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_6 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_6_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_6_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_6 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_6_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_6_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_6 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_7_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_7_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_7 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_7_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_7_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_7 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_7_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_7_id ON unix_tenant_202605.ts_iot_device_log_metadata_7 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_7_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_7_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_7 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_7_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_7_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_7 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_7_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_7_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_7 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_8_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_8_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_8 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_8_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_8_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_8 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_8_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_8_id ON unix_tenant_202605.ts_iot_device_log_metadata_8 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_8_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_8_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_8 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_8_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_8_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_8 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_8_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_8_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_8 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_9_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_9_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_9 USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_9_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_9_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata_9 USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_9_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_9_id ON unix_tenant_202605.ts_iot_device_log_metadata_9 USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_9_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_9_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata_9 USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_9_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_9_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata_9 USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_9_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_9_message_time ON unix_tenant_202605.ts_iot_device_log_metadata_9 USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_history_event_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_history_event_v2 ON unix_tenant_202605.ts_iot_device_log_metadata USING btree (iot_id, event, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_history_property_v2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_history_property_v2 ON unix_tenant_202605.ts_iot_device_log_metadata USING btree (iot_id, property, create_time DESC, id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_id ON unix_tenant_202605.ts_iot_device_log_metadata USING btree (id DESC);


--
-- Name: idx_ts_iot_device_log_metadata_iot_event_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_iot_event_time ON unix_tenant_202605.ts_iot_device_log_metadata USING btree (iot_id, event, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_iot_property_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_iot_property_time ON unix_tenant_202605.ts_iot_device_log_metadata USING btree (iot_id, property, create_time DESC);


--
-- Name: idx_ts_iot_device_log_metadata_message_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_metadata_message_time ON unix_tenant_202605.ts_iot_device_log_metadata USING btree (message_type, create_time DESC);


--
-- Name: idx_ts_iot_device_log_product_device_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_ts_iot_device_log_product_device_time ON unix_tenant_202605.ts_iot_device_log USING btree (product_key, device_id, create_time DESC);


--
-- Name: idx_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_type ON unix_tenant_202605.iot_resource_connection USING btree (type);


--
-- Name: idx_type_category; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_type_category ON unix_tenant_202605.mp_component_library USING btree (component_type, category);


--
-- Name: idx_unq_client_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX idx_unq_client_id ON unix_tenant_202605.oauth_client_details USING btree (client_id);


--
-- Name: idx_use_count; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_use_count ON unix_tenant_202605.iot_standard_metadata_field USING btree (use_count DESC);


--
-- Name: idx_used; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_used ON unix_tenant_202605.mp_diy_template USING btree (used);


--
-- Name: idx_user_app; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_user_app ON unix_tenant_202605.mp_user_permission USING btree (user_id, app_id);


--
-- Name: idx_user_app_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_user_app_tenant ON unix_tenant_202605.iot_user_application USING btree (tenant_id, app_status, deleted);


--
-- Name: idx_user_sub_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_user_sub_tenant ON unix_tenant_202605.iot_user_sub USING btree (tenant_id, status, deleted);


--
-- Name: idx_user_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_user_tenant ON unix_tenant_202605.iot_user USING btree (tenant_id, status, deleted);


--
-- Name: idx_user_tenant_dept; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_user_tenant_dept ON unix_tenant_202605.iot_user USING btree (tenant_id, dept_id, status, deleted);


--
-- Name: idx_value_type; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_value_type ON unix_tenant_202605.mp_component_type_mapping USING btree (value_type);


--
-- Name: idx_video_ai_algorithm_log_task; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_algorithm_log_task ON unix_tenant_202605.video_ai_algorithm_task_log USING btree (task_id, occurred_at);


--
-- Name: idx_video_ai_algorithm_result_alarm; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_algorithm_result_alarm ON unix_tenant_202605.video_ai_algorithm_task_result USING btree (tenant_id, alarm_status, frame_time);


--
-- Name: idx_video_ai_algorithm_result_recent; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_algorithm_result_recent ON unix_tenant_202605.video_ai_algorithm_task_result USING btree (task_id, frame_time DESC, id DESC);


--
-- Name: idx_video_ai_algorithm_result_source; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_algorithm_result_source ON unix_tenant_202605.video_ai_algorithm_task_result USING btree (task_id, source_key, frame_time DESC);


--
-- Name: idx_video_ai_algorithm_result_task; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_algorithm_result_task ON unix_tenant_202605.video_ai_algorithm_task_result USING btree (task_id, frame_time);


--
-- Name: idx_video_ai_algorithm_result_task_alarm_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_algorithm_result_task_alarm_time ON unix_tenant_202605.video_ai_algorithm_task_result USING btree (task_id, alarm_status, frame_time DESC, id DESC);


--
-- Name: idx_video_ai_algorithm_task_runtime; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_algorithm_task_runtime ON unix_tenant_202605.video_ai_algorithm_task USING btree (runtime_node_id, runtime_task_id, status);


--
-- Name: idx_video_ai_algorithm_task_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_algorithm_task_tenant ON unix_tenant_202605.video_ai_algorithm_task USING btree (tenant_id, deleted, task_type, status);


--
-- Name: idx_video_ai_candidate_dataset; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_candidate_dataset ON unix_tenant_202605.video_ai_annotation_candidate USING btree (dataset_id, deleted, status);


--
-- Name: idx_video_ai_candidate_item; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_candidate_item ON unix_tenant_202605.video_ai_annotation_candidate USING btree (item_id, deleted, status);


--
-- Name: idx_video_ai_candidate_task; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_candidate_task ON unix_tenant_202605.video_ai_annotation_candidate USING btree (task_id, deleted, status);


--
-- Name: idx_video_ai_dataset_artifact; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_dataset_artifact ON unix_tenant_202605.video_ai_dataset_artifact USING btree (dataset_id, dataset_version_id, artifact_type, deleted);


--
-- Name: idx_video_ai_dataset_capture_dataset; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_dataset_capture_dataset ON unix_tenant_202605.video_ai_dataset_capture_job USING btree (dataset_id, deleted, status);


--
-- Name: idx_video_ai_dataset_capture_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_dataset_capture_list ON unix_tenant_202605.video_ai_dataset_capture_job USING btree (dataset_id, deleted, create_time DESC, id DESC);


--
-- Name: idx_video_ai_dataset_capture_replaces; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_dataset_capture_replaces ON unix_tenant_202605.video_ai_dataset_capture_job USING btree (replaces_job_id, deleted, status);


--
-- Name: idx_video_ai_dataset_capture_runtime; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_dataset_capture_runtime ON unix_tenant_202605.video_ai_dataset_capture_job USING btree (runtime_task_id, runtime_node_id);


--
-- Name: idx_video_ai_dataset_capture_source; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_dataset_capture_source ON unix_tenant_202605.video_ai_dataset_capture_job USING btree (tenant_id, source_fingerprint, deleted, status);


--
-- Name: idx_video_ai_dataset_capture_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_dataset_capture_tenant ON unix_tenant_202605.video_ai_dataset_capture_job USING btree (tenant_id, deleted, status);


--
-- Name: idx_video_ai_dataset_class_dataset; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_dataset_class_dataset ON unix_tenant_202605.video_ai_dataset_class USING btree (dataset_id, deleted);


--
-- Name: idx_video_ai_dataset_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_dataset_list ON unix_tenant_202605.video_ai_dataset USING btree (tenant_id, deleted, update_time DESC, id DESC);


--
-- Name: idx_video_ai_dataset_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_dataset_tenant ON unix_tenant_202605.video_ai_dataset USING btree (tenant_id, deleted, status);


--
-- Name: idx_video_ai_dataset_version_current; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_dataset_version_current ON unix_tenant_202605.video_ai_dataset_version USING btree (dataset_id, deleted, version_status);


--
-- Name: idx_video_ai_deploy_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_deploy_tenant ON unix_tenant_202605.video_ai_deploy_service USING btree (tenant_id, deleted, status);


--
-- Name: idx_video_ai_frame_candidate_dataset; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_frame_candidate_dataset ON unix_tenant_202605.video_ai_dataset_frame_candidate USING btree (dataset_id, deleted, candidate_status);


--
-- Name: idx_video_ai_frame_candidate_job; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_frame_candidate_job ON unix_tenant_202605.video_ai_dataset_frame_candidate USING btree (capture_job_id, deleted, candidate_status, source_frame_time DESC, id DESC);


--
-- Name: idx_video_ai_frame_candidate_queue; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_frame_candidate_queue ON unix_tenant_202605.video_ai_dataset_frame_candidate USING btree (dataset_id, deleted, candidate_status, source_frame_time DESC, id DESC);


--
-- Name: idx_video_ai_frame_candidate_source; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_frame_candidate_source ON unix_tenant_202605.video_ai_dataset_frame_candidate USING btree (tenant_id, source_key, source_frame_time DESC);


--
-- Name: idx_video_ai_frame_candidate_target; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_frame_candidate_target ON unix_tenant_202605.video_ai_dataset_frame_candidate USING btree (dataset_id, deleted, candidate_status, detection_count DESC, source_frame_time DESC, id DESC);


--
-- Name: idx_video_ai_frame_extractor_enabled; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_frame_extractor_enabled ON unix_tenant_202605.video_ai_frame_extractor USING btree (tenant_id, deleted, enabled);


--
-- Name: idx_video_ai_item_annotation_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_item_annotation_status ON unix_tenant_202605.video_ai_dataset_item USING btree (dataset_id, deleted, annotation_status);


--
-- Name: idx_video_ai_item_capture_job; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_item_capture_job ON unix_tenant_202605.video_ai_dataset_item USING btree (capture_job_id, deleted);


--
-- Name: idx_video_ai_item_dataset; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_item_dataset ON unix_tenant_202605.video_ai_dataset_item USING btree (dataset_id, deleted, completed);


--
-- Name: idx_video_ai_item_list; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_item_list ON unix_tenant_202605.video_ai_dataset_item USING btree (dataset_id, deleted, update_time DESC, id DESC);


--
-- Name: idx_video_ai_item_sample_state; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_item_sample_state ON unix_tenant_202605.video_ai_dataset_item USING btree (dataset_id, deleted, sample_status, review_status, sample_role);


--
-- Name: idx_video_ai_item_source_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_item_source_time ON unix_tenant_202605.video_ai_dataset_item USING btree (dataset_id, source_type, source_frame_time DESC);


--
-- Name: idx_video_ai_item_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_item_tenant ON unix_tenant_202605.video_ai_dataset_item USING btree (tenant_id, deleted);


--
-- Name: idx_video_ai_item_version; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_item_version ON unix_tenant_202605.video_ai_dataset_item USING btree (dataset_version_id, deleted, split_type);


--
-- Name: idx_video_ai_media_server_role; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_media_server_role ON unix_tenant_202605.video_ai_media_server USING btree (deleted, enabled, stream_role, status, health_status);


--
-- Name: idx_video_ai_media_server_schedule; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_media_server_schedule ON unix_tenant_202605.video_ai_media_server USING btree (deleted, enabled, priority, load_score, active_push_streams, active_pull_streams);


--
-- Name: idx_video_ai_media_server_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_media_server_tenant ON unix_tenant_202605.video_ai_media_server USING btree (tenant_id, deleted, status);


--
-- Name: idx_video_ai_model_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_model_tenant ON unix_tenant_202605.video_ai_model USING btree (tenant_id, deleted, status);


--
-- Name: idx_video_ai_quality_report; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_quality_report ON unix_tenant_202605.video_ai_dataset_quality_report USING btree (dataset_id, dataset_version_id, deleted);


--
-- Name: idx_video_ai_result_sorter_enabled; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_result_sorter_enabled ON unix_tenant_202605.video_ai_result_sorter USING btree (tenant_id, deleted, enabled);


--
-- Name: idx_video_ai_runtime_node_health; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_runtime_node_health ON unix_tenant_202605.video_ai_runtime_node USING btree (deleted, health_status, last_heartbeat);


--
-- Name: idx_video_ai_runtime_node_schedule; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_runtime_node_schedule ON unix_tenant_202605.video_ai_runtime_node USING btree (deleted, enabled, status, health_status, priority, running_task_count);


--
-- Name: idx_video_ai_runtime_node_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_runtime_node_tenant ON unix_tenant_202605.video_ai_runtime_node USING btree (tenant_id, deleted, status);


--
-- Name: idx_video_ai_sample_annotation_dataset; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_sample_annotation_dataset ON unix_tenant_202605.video_ai_sample_annotation USING btree (dataset_id, dataset_version_id, deleted);


--
-- Name: idx_video_ai_sample_annotation_item; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_sample_annotation_item ON unix_tenant_202605.video_ai_sample_annotation USING btree (dataset_item_id, deleted, status);


--
-- Name: idx_video_ai_task_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_task_tenant ON unix_tenant_202605.video_ai_task USING btree (tenant_id, deleted, task_type, status);


--
-- Name: idx_video_ai_task_tenant_type_status_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_task_tenant_type_status_time ON unix_tenant_202605.video_ai_task USING btree (tenant_id, task_type, status, create_time DESC, id DESC) WHERE (deleted = 0);


--
-- Name: idx_video_ai_task_tenant_type_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_task_tenant_type_time ON unix_tenant_202605.video_ai_task USING btree (tenant_id, task_type, create_time DESC, id DESC) WHERE (deleted = 0);


--
-- Name: idx_video_ai_task_type_status_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_task_type_status_time ON unix_tenant_202605.video_ai_task USING btree (task_type, status, create_time DESC, id DESC) WHERE (deleted = 0);


--
-- Name: idx_video_ai_task_type_time; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_ai_task_type_time ON unix_tenant_202605.video_ai_task USING btree (task_type, create_time DESC, id DESC) WHERE (deleted = 0);


--
-- Name: idx_video_channel_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_channel_tenant ON unix_tenant_202605.video_platform_channel USING btree (tenant_id, instance_key, channel_status);


--
-- Name: idx_video_device_cache_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_device_cache_tenant ON unix_tenant_202605.video_platform_device_cache USING btree (tenant_id, instance_key, status);


--
-- Name: idx_video_device_ext_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_device_ext_tenant ON unix_tenant_202605.video_platform_device_ext USING btree (tenant_id, instance_key);


--
-- Name: idx_video_device_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_device_tenant ON unix_tenant_202605.video_platform_device USING btree (tenant_id, instance_key, device_status);


--
-- Name: idx_video_instance_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_instance_tenant ON unix_tenant_202605.video_platform_instance USING btree (tenant_id, enabled);


--
-- Name: idx_video_org_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_video_org_tenant ON unix_tenant_202605.video_platform_org_cache USING btree (tenant_id, instance_key);


--
-- Name: idx_visual_banner_tenant_status; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_visual_banner_tenant_status ON unix_tenant_202605.iot_visual_banner USING btree (tenant_id, status, create_time);


--
-- Name: idx_viz_widget_bundle_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_viz_widget_bundle_tenant ON unix_tenant_202605.iot_viz_widget_bundle USING btree (tenant_id, status);


--
-- Name: idx_viz_widget_type_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_viz_widget_type_tenant ON unix_tenant_202605.iot_viz_widget_type USING btree (tenant_id, status);


--
-- Name: iot_device_log_0_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202603_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_0_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_0_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_0_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_0_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202604_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_0_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_0_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_0_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_0_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202605_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_0_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_0_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_0_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_0_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202606_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_0_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_0_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_0_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_0_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202607_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_0_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_0_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_0_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_0_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202608_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_0_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_0_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_0_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_0_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202609_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_0_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_0_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_0_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_0_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_10_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_10_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_10_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_10_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_10_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_10_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_10_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_10_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_10_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_10_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_10_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_10_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_10_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_10_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_10_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_10_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_10_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_10_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_10_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_10_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_10_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_10_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_10_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_10_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_10_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_10_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_10_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_10_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_10_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_11_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_11_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_11_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_11_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_11_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_11_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_11_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_11_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_11_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_11_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_11_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_11_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_11_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_11_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_11_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_11_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_11_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_11_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_11_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_11_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_11_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_11_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_11_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_11_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_11_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_11_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_11_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_11_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_11_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_12_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_12_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_12_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_12_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_12_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_12_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_12_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_12_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_12_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_12_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_12_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_12_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_12_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_12_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_12_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_12_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_12_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_12_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_12_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_12_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_12_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_12_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_12_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_12_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_12_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_12_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_12_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_12_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_12_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_13_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_13_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_13_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_13_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_13_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_13_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_13_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_13_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_13_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_13_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_13_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_13_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_13_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_13_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_13_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_13_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_13_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_13_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_13_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_13_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_13_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_13_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_13_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_13_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_13_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_13_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_13_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_13_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_13_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_14_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_14_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_14_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_14_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_14_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_14_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_14_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_14_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_14_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_14_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_14_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_14_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_14_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_14_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_14_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_14_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_14_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_14_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_14_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_14_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_14_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_14_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_14_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_14_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_14_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_14_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_14_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_14_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_14_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_15_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_15_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_15_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_15_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_15_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_15_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_15_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_15_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_15_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_15_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_15_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_15_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_15_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_15_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_15_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_15_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_15_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_15_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_15_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_15_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_15_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_15_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_15_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_15_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_15_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_15_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_15_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_15_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_15_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_16_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_16_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_16_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_16_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_16_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_16_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_16_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_16_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_16_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_16_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_16_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_16_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_16_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_16_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_16_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_16_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_16_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_16_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_16_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_16_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_16_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_16_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_16_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_16_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_16_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_16_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_16_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_16_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_16_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_17_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_17_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_17_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_17_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_17_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_17_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_17_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_17_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_17_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_17_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_17_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_17_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_17_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_17_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_17_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_17_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_17_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_17_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_17_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_17_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_17_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_17_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_17_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_17_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_17_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_17_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_17_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_17_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_17_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_18_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_18_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_18_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_18_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_18_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_18_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_18_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_18_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_18_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_18_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_18_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_18_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_18_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_18_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_18_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_18_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_18_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_18_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_18_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_18_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_18_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_18_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_18_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_18_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_18_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_18_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_18_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_18_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_18_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_19_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_19_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_19_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_19_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_19_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_19_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_19_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_19_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_19_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_19_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_19_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_19_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_19_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_19_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_19_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_19_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_19_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_19_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_19_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_19_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_19_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_19_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_19_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_19_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_19_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_19_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_19_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_19_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_19_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_1_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202603_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_1_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_1_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_1_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_1_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202604_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_1_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_1_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_1_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_1_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202605_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_1_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_1_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_1_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_1_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202606_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_1_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_1_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_1_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_1_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202607_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_1_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_1_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_1_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_1_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202608_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_1_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_1_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_1_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_1_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202609_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_1_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_1_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_1_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_1_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_20_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_20_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_20_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_20_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_20_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_20_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_20_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_20_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_20_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_20_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_20_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_20_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_20_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_20_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_20_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_20_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_20_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_20_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_20_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_20_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_20_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_20_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_20_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_20_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_20_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_20_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_20_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_20_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_20_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_21_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_21_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_21_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_21_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_21_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_21_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_21_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_21_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_21_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_21_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_21_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_21_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_21_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_21_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_21_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_21_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_21_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_21_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_21_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_21_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_21_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_21_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_21_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_21_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_21_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_21_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_21_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_21_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_21_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_22_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_22_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_22_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_22_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_22_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_22_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_22_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_22_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_22_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_22_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_22_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_22_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_22_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_22_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_22_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_22_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_22_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_22_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_22_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_22_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_22_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_22_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_22_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_22_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_22_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_22_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_22_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_22_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_22_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_23_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202603_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_23_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_23_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_23_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_23_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202604_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_23_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_23_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_23_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_23_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202605_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_23_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_23_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_23_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_23_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202606_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_23_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_23_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_23_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_23_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202607_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_23_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_23_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_23_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_23_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202608_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_23_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_23_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_23_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_23_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202609_iot_id_message_type_create_time_e_idx ON unix_tenant_202605.iot_device_log_23_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_23_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_23_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_23_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_2_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202603_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_2_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_2_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_2_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_2_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202604_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_2_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_2_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_2_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_2_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202605_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_2_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_2_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_2_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_2_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202606_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_2_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_2_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_2_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_2_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202607_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_2_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_2_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_2_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_2_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202608_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_2_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_2_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_2_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_2_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202609_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_2_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_2_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_2_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_2_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_3_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202603_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_3_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_3_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_3_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_3_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202604_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_3_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_3_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_3_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_3_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202605_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_3_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_3_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_3_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_3_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202606_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_3_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_3_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_3_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_3_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202607_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_3_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_3_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_3_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_3_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202608_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_3_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_3_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_3_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_3_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202609_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_3_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_3_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_3_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_3_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_4_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202603_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_4_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_4_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_4_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_4_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202604_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_4_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_4_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_4_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_4_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202605_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_4_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_4_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_4_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_4_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202606_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_4_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_4_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_4_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_4_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202607_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_4_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_4_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_4_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_4_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202608_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_4_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_4_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_4_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_4_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202609_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_4_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_4_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_4_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_4_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_5_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202603_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_5_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_5_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_5_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_5_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202604_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_5_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_5_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_5_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_5_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202605_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_5_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_5_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_5_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_5_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202606_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_5_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_5_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_5_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_5_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202607_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_5_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_5_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_5_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_5_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202608_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_5_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_5_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_5_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_5_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202609_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_5_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_5_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_5_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_5_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_6_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202603_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_6_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_6_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_6_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_6_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202604_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_6_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_6_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_6_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_6_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202605_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_6_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_6_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_6_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_6_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202606_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_6_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_6_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_6_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_6_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202607_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_6_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_6_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_6_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_6_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202608_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_6_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_6_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_6_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_6_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202609_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_6_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_6_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_6_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_6_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_7_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202603_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_7_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_7_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_7_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_7_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202604_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_7_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_7_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_7_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_7_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202605_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_7_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_7_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_7_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_7_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202606_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_7_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_7_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_7_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_7_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202607_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_7_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_7_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_7_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_7_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202608_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_7_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_7_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_7_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_7_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202609_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_7_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_7_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_7_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_7_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_8_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202603_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_8_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_8_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_8_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_8_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202604_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_8_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_8_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_8_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_8_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202605_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_8_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_8_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_8_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_8_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202606_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_8_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_8_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_8_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_8_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202607_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_8_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_8_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_8_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_8_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202608_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_8_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_8_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_8_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_8_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202609_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_8_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_8_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_8_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_8_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_9_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202603_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_9_p202603 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_9_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_9_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_9_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202604_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_9_p202604 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_9_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_9_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_9_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202605_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_9_p202605 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_9_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_9_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_9_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202606_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_9_p202606 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_9_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_9_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_9_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202607_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_9_p202607 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_9_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_9_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_9_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202608_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_9_p202608 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_9_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_9_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_9_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202609_iot_id_message_type_create_time_ev_idx ON unix_tenant_202605.iot_device_log_9_p202609 USING btree (iot_id, message_type, create_time, event);


--
-- Name: iot_device_log_9_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_9_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_9_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_0_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_0_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_0_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_0_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_0_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_0_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_0_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_0_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_0_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_0_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_0_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_0_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_0_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_0_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p20_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_0_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p20_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_0_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p20_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_0_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p20_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_0_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p20_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_0_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p20_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_0_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_0_p20_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_0_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_10_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_10_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_10_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_10_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_10_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_10_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_10_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_10_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_10_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_10_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_10_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_10_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_10_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_10_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_10_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_10_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_10_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_10_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_10_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_10_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_10_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_10_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_11_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_11_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_11_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_11_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_11_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_11_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_11_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_11_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_11_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_11_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_11_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_11_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_11_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_11_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_11_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_11_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_11_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_11_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_11_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_11_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_11_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_11_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_12_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_12_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_12_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_12_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_12_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_12_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_12_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_12_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_12_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_12_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_12_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_12_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_12_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_12_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_12_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_12_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_12_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_12_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_12_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_12_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_12_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_12_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_13_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_13_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_13_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_13_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_13_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_13_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_13_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_13_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_13_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_13_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_13_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_13_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_13_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_13_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_13_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_13_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_13_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_13_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_13_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_13_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_13_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_13_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_14_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_14_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_14_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_14_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_14_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_14_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_14_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_14_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_14_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_14_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_14_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_14_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_14_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_14_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_14_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_14_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_14_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_14_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_14_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_14_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_14_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_14_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_15_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_15_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_15_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_15_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_15_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_15_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_15_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_15_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_15_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_15_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_15_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_15_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_15_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_15_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_15_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_15_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_15_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_15_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_15_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_15_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_15_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_15_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_16_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_16_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_16_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_16_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_16_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_16_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_16_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_16_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_16_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_16_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_16_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_16_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_16_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_16_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_16_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_16_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_16_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_16_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_16_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_16_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_16_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_16_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_17_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_17_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_17_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_17_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_17_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_17_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_17_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_17_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_17_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_17_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_17_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_17_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_17_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_17_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_17_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_17_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_17_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_17_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_17_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_17_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_17_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_17_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_18_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_18_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_18_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_18_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_18_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_18_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_18_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_18_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_18_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_18_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_18_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_18_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_18_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_18_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_18_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_18_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_18_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_18_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_18_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_18_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_18_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_18_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_19_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_19_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_19_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_19_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_19_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_19_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_19_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_19_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_19_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_19_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_19_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_19_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_19_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_19_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_19_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_19_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_19_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_19_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_19_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_19_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_19_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_19_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_1_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_1_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_1_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_1_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_1_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_1_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_1_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_1_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_1_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_1_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_1_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_1_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_1_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_1_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p20_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_1_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p20_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_1_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p20_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_1_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p20_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_1_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p20_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_1_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p20_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_1_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_1_p20_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_1_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_20_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_20_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_20_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_20_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_20_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_20_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_20_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_20_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_20_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_20_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_20_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_20_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_20_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_20_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_20_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_20_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_20_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_20_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_20_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_20_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_20_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_20_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_21_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_21_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_21_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_21_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_21_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_21_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_21_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_21_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_21_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_21_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_21_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_21_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_21_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_21_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_21_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_21_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_21_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_21_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_21_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_21_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_21_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_21_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_22_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_22_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_22_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_22_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_22_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_22_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_22_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_22_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_22_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_22_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_22_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_22_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_22_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_22_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_22_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_22_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_22_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_22_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_22_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_22_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_22_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_22_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_23_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_23_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_23_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_23_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_23_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_23_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_23_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_23_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_23_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_23_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_23_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_23_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_23_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_23_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p2_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_23_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p2_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_23_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p2_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_23_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p2_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_23_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p2_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_23_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p2_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_23_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_23_p2_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_23_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_2_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_2_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_2_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_2_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_2_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_2_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_2_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_2_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_2_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_2_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_2_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_2_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_2_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_2_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p20_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_2_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p20_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_2_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p20_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_2_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p20_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_2_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p20_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_2_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p20_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_2_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_2_p20_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_2_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_3_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_3_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_3_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_3_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_3_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_3_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_3_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_3_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_3_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_3_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_3_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_3_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_3_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_3_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p20_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_3_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p20_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_3_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p20_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_3_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p20_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_3_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p20_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_3_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p20_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_3_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_3_p20_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_3_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_4_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_4_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_4_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_4_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_4_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_4_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_4_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_4_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_4_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_4_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_4_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_4_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_4_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_4_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p20_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_4_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p20_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_4_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p20_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_4_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p20_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_4_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p20_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_4_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p20_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_4_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_4_p20_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_4_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_5_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_5_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_5_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_5_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_5_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_5_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_5_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_5_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_5_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_5_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_5_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_5_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_5_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_5_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p20_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_5_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p20_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_5_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p20_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_5_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p20_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_5_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p20_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_5_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p20_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_5_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_5_p20_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_5_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_6_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_6_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_6_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_6_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_6_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_6_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_6_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_6_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_6_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_6_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_6_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_6_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_6_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_6_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p20_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_6_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p20_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_6_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p20_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_6_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p20_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_6_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p20_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_6_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p20_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_6_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_6_p20_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_6_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_7_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_7_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_7_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_7_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_7_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_7_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_7_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_7_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_7_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_7_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_7_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_7_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_7_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_7_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p20_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_7_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p20_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_7_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p20_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_7_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p20_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_7_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p20_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_7_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p20_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_7_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_7_p20_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_7_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_8_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_8_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_8_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_8_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_8_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_8_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_8_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_8_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_8_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_8_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_8_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_8_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_8_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_8_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p20_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_8_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p20_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_8_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p20_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_8_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p20_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_8_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p20_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_8_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p20_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_8_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_8_p20_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_8_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_9_p202603_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p202603_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_9_p202603 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_9_p202604_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p202604_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_9_p202604 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_9_p202605_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p202605_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_9_p202605 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_9_p202606_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p202606_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_9_p202606 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_9_p202607_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p202607_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_9_p202607 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_9_p202608_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p202608_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_9_p202608 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_9_p202609_tenant_id_create_time_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p202609_tenant_id_create_time_idx ON unix_tenant_202605.iot_device_log_metadata_9_p202609 USING btree (tenant_id, create_time);


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property__idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p20_iot_id_message_type_property__idx ON unix_tenant_202605.iot_device_log_metadata_9_p202603 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property_idx1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p20_iot_id_message_type_property_idx1 ON unix_tenant_202605.iot_device_log_metadata_9_p202604 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property_idx2; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p20_iot_id_message_type_property_idx2 ON unix_tenant_202605.iot_device_log_metadata_9_p202605 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property_idx3; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p20_iot_id_message_type_property_idx3 ON unix_tenant_202605.iot_device_log_metadata_9_p202606 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property_idx4; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p20_iot_id_message_type_property_idx4 ON unix_tenant_202605.iot_device_log_metadata_9_p202607 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property_idx5; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p20_iot_id_message_type_property_idx5 ON unix_tenant_202605.iot_device_log_metadata_9_p202608 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property_idx6; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_device_log_metadata_9_p20_iot_id_message_type_property_idx6 ON unix_tenant_202605.iot_device_log_metadata_9_p202609 USING btree (iot_id, message_type, property, event, id);


--
-- Name: iot_pd; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX iot_pd ON unix_tenant_202605.iot_device_subscribe USING btree (product_key, device_id);


--
-- Name: map; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX map ON unix_tenant_202605.iot_visual_map USING btree (id, parent_id, code, parent_code);


--
-- Name: pid_index; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX pid_index ON unix_tenant_202605.support_map_areas USING btree (pid);


--
-- Name: sched_name; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX sched_name ON unix_tenant_202605.qrtz_triggers USING btree (sched_name, job_name, job_group);


--
-- Name: uk_ai_health_profile; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_ai_health_profile ON unix_tenant_202605.ai_model_health USING btree (model_profile_id);


--
-- Name: uk_ai_profile_code_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_ai_profile_code_tenant ON unix_tenant_202605.ai_model_profile USING btree (tenant_id, model_code);


--
-- Name: uk_ai_prompt_code_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_ai_prompt_code_tenant ON unix_tenant_202605.ai_prompt_template USING btree (tenant_id, template_code, version);


--
-- Name: uk_ai_provider_code_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_ai_provider_code_tenant ON unix_tenant_202605.ai_model_provider USING btree (tenant_id, provider_code);


--
-- Name: uk_app_product; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_app_product ON unix_tenant_202605.mp_detail_layout USING btree (app_id, product_key);


--
-- Name: uk_app_user_iot; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_app_user_iot ON unix_tenant_202605.mp_user_device USING btree (app_id, user_id, iot_id);


--
-- Name: uk_app_username; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_app_username ON unix_tenant_202605.mp_user USING btree (app_id, username);


--
-- Name: uk_category_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_category_key ON unix_tenant_202605.iot_standard_category USING btree (tenant_id, category_key);


--
-- Name: uk_component_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_component_key ON unix_tenant_202605.mp_component_library USING btree (component_key);


--
-- Name: uk_config_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_config_id ON unix_tenant_202605.iot_data_input_task USING btree (config_id);


--
-- Name: uk_date_product; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_date_product ON unix_tenant_202605.iot_device_statistics USING btree (stat_date, product_key);


--
-- Name: uk_date_product_channel_metric; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_date_product_channel_metric ON unix_tenant_202605.iot_dashboard_statistics USING btree (stat_date, product_key, channel, metric_type);


--
-- Name: uk_device_channel; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_device_channel ON unix_tenant_202605.video_platform_channel USING btree (device_id, channel_id);


--
-- Name: uk_device_instance; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_device_instance ON unix_tenant_202605.video_platform_device_ext USING btree (device_id, instance_key);


--
-- Name: uk_device_task; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_device_task ON unix_tenant_202605.iot_gateway_polling_config USING btree (product_key, device_id, task_type);


--
-- Name: uk_field_category; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_field_category ON unix_tenant_202605.iot_standard_field_category_rel USING btree (field_id, category_id);


--
-- Name: uk_field_type_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_field_type_id ON unix_tenant_202605.iot_standard_metadata_field USING btree (tenant_id, field_type, field_id);


--
-- Name: idx_instance_channel; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE INDEX idx_instance_channel ON unix_tenant_202605.video_platform_channel USING btree (instance_key, channel_id, parent_id);


--
-- Name: uk_instance_device; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_instance_device ON unix_tenant_202605.video_platform_device USING btree (instance_key, device_id);


--
-- Name: uk_instance_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_instance_key ON unix_tenant_202605.video_platform_instance USING btree (instance_key);


--
-- Name: uk_instance_org; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_instance_org ON unix_tenant_202605.video_platform_org_cache USING btree (instance_key, org_id);


--
-- Name: uk_instance_platform_device; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_instance_platform_device ON unix_tenant_202605.video_platform_device_ext USING btree (instance_key, platform_device_id);


--
-- Name: uk_iot_analysis_page_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_iot_analysis_page_code ON unix_tenant_202605.iot_analysis_page USING btree (page_code);


--
-- Name: uk_iot_analysis_template_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_iot_analysis_template_code ON unix_tenant_202605.iot_analysis_template USING btree (template_code);


--
-- Name: uk_iot_certificate_ssl_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_iot_certificate_ssl_key ON unix_tenant_202605.iot_certificate USING btree (ssl_key);


--
-- Name: uk_iot_viz_dashboard_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_iot_viz_dashboard_code ON unix_tenant_202605.iot_viz_dashboard USING btree (dashboard_code);


--
-- Name: uk_iot_viz_import_record_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_iot_viz_import_record_code ON unix_tenant_202605.iot_viz_import_record USING btree (import_code);


--
-- Name: uk_iot_viz_widget_bundle_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_iot_viz_widget_bundle_code ON unix_tenant_202605.iot_viz_widget_bundle USING btree (bundle_code);


--
-- Name: uk_iot_viz_widget_type_fqn; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_iot_viz_widget_type_fqn ON unix_tenant_202605.iot_viz_widget_type USING btree (full_fqn);


--
-- Name: uk_node_red_binding_device; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_node_red_binding_device ON unix_tenant_202605.iot_node_red_binding USING btree (instance_id, product_key, device_id);


--
-- Name: uk_node_red_deploy_token_hash; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_node_red_deploy_token_hash ON unix_tenant_202605.iot_node_red_deployment_token USING btree (token_hash);


--
-- Name: uk_node_red_tenant_edge; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_node_red_tenant_edge ON unix_tenant_202605.iot_node_red_instance USING btree (tenant_id, edge_id);


--
-- Name: uk_node_red_token_hash; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_node_red_token_hash ON unix_tenant_202605.iot_node_red_credential USING btree (token_hash);


--
-- Name: uk_notice_wechat_app_openid; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_notice_wechat_app_openid ON unix_tenant_202605.notice_wechat_openid USING btree (app_id, openid);


--
-- Name: uk_notice_wechat_app_user; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_notice_wechat_app_user ON unix_tenant_202605.notice_wechat_openid USING btree (app_id, user_key);


--
-- Name: uk_oat_token; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_oat_token ON unix_tenant_202605.oauth2_access_token USING btree (access_token);


--
-- Name: uk_ort_token; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_ort_token ON unix_tenant_202605.oauth2_refresh_token USING btree (refresh_token);


--
-- Name: uk_ota_device_version; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_ota_device_version ON unix_tenant_202605.iot_ota_device_version USING btree (product_key, device_id, package_type, module);


--
-- Name: uk_ota_task_device; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_ota_task_device ON unix_tenant_202605.iot_ota_task_device USING btree (task_id, product_key, device_id);


--
-- Name: uk_ota_task_no; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_ota_task_no ON unix_tenant_202605.iot_ota_task USING btree (task_no);


--
-- Name: uk_package_tag; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_package_tag ON unix_tenant_202605.iot_ota_package_tag_rel USING btree (package_id, tag_id);


--
-- Name: uk_product_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_product_key ON unix_tenant_202605.iot_standard_product USING btree (tenant_id, product_key);


--
-- Name: uk_product_version; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_product_version ON unix_tenant_202605.iot_product_metadata_version USING btree (product_id, version);


--
-- Name: uk_rulego_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_rulego_id ON unix_tenant_202605.rulego_chain USING btree (rulego_id);


--
-- Name: uk_sub_account_permission_scope; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_sub_account_permission_scope ON unix_tenant_202605.iot_sub_account_permission USING btree (union_id, resource_type, resource_id);


--
-- Name: uk_sys_oss_config_tenant_active; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_sys_oss_config_tenant_active ON unix_tenant_202605.sys_oss_config USING btree (COALESCE(tenant_id, ''::character varying)) WHERE (active = true);


--
-- Name: uk_sys_oss_config_tenant_provider; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_sys_oss_config_tenant_provider ON unix_tenant_202605.sys_oss_config USING btree (COALESCE(tenant_id, ''::character varying), provider);


--
-- Name: uk_tag_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_tag_code ON unix_tenant_202605.iot_ota_tag USING btree (tag_code);


--
-- Name: uk_template_category_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_template_category_code ON unix_tenant_202605.iot_node_red_flow_template_category USING btree (code);


--
-- Name: uk_template_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_template_key ON unix_tenant_202605.mp_layout_template USING btree (template_key);


--
-- Name: uk_type_component; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_type_component ON unix_tenant_202605.mp_component_type_mapping USING btree (value_type, component_key);


--
-- Name: uk_user_perm; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_user_perm ON unix_tenant_202605.mp_user_permission USING btree (user_id, app_id, permission_code);


--
-- Name: uk_video_ai_algorithm_log_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_algorithm_log_key ON unix_tenant_202605.video_ai_algorithm_task_log USING btree (task_id, log_key);


--
-- Name: uk_video_ai_algorithm_result_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_algorithm_result_key ON unix_tenant_202605.video_ai_algorithm_task_result USING btree (task_id, result_key);


--
-- Name: uk_video_ai_algorithm_task_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_algorithm_task_code ON unix_tenant_202605.video_ai_algorithm_task USING btree (task_code);


--
-- Name: uk_video_ai_dataset_capture_active_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_dataset_capture_active_key ON unix_tenant_202605.video_ai_dataset_capture_job USING btree (active_source_key) WHERE (active_source_key IS NOT NULL);


--
-- Name: uk_video_ai_dataset_capture_job_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_dataset_capture_job_code ON unix_tenant_202605.video_ai_dataset_capture_job USING btree (job_code);


--
-- Name: uk_video_ai_dataset_class_idx; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_dataset_class_idx ON unix_tenant_202605.video_ai_dataset_class USING btree (dataset_id, dataset_version_id, class_index, deleted);


--
-- Name: uk_video_ai_dataset_code_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_dataset_code_tenant ON unix_tenant_202605.video_ai_dataset USING btree (tenant_id, dataset_code);


--
-- Name: uk_video_ai_dataset_version_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_dataset_version_code ON unix_tenant_202605.video_ai_dataset_version USING btree (dataset_id, version_code);


--
-- Name: uk_video_ai_frame_candidate_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_frame_candidate_key ON unix_tenant_202605.video_ai_dataset_frame_candidate USING btree (tenant_id, dataset_id, capture_job_id, source_frame_key) WHERE ((deleted = 0) AND (source_frame_key IS NOT NULL));


--
-- Name: uk_video_ai_frame_extractor_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_frame_extractor_code ON unix_tenant_202605.video_ai_frame_extractor USING btree (tenant_id, extractor_code);


--
-- Name: uk_video_ai_item_capture_frame; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_item_capture_frame ON unix_tenant_202605.video_ai_dataset_item USING btree (tenant_id, dataset_id, capture_job_id, source_frame_key) WHERE ((deleted = 0) AND (capture_job_id IS NOT NULL) AND (source_frame_key IS NOT NULL));


--
-- Name: uk_video_ai_media_server_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_media_server_code ON unix_tenant_202605.video_ai_media_server USING btree (server_code);


--
-- Name: uk_video_ai_model_code_tenant; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_model_code_tenant ON unix_tenant_202605.video_ai_model USING btree (tenant_id, model_code);


--
-- Name: uk_video_ai_result_sorter_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_result_sorter_code ON unix_tenant_202605.video_ai_result_sorter USING btree (tenant_id, sorter_code);


--
-- Name: uk_video_ai_runtime_node_code; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uk_video_ai_runtime_node_code ON unix_tenant_202605.video_ai_runtime_node USING btree (node_code);


--
-- Name: uniq_network_pk_id; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uniq_network_pk_id ON unix_tenant_202605.iot_network1 USING btree (union_id, type);


--
-- Name: uniq_network_pk_id_copy1; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uniq_network_pk_id_copy1 ON unix_tenant_202605.iot_network USING btree (union_id, type);


--
-- Name: uniq_ssl_key; Type: INDEX; Schema: unix_tenant_202605; Owner: -
--

CREATE UNIQUE INDEX uniq_ssl_key ON unix_tenant_202605.iot_certificate USING btree (ssl_key);


--
-- Name: iot_device_log_0_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202603_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_0_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202603_pkey;


--
-- Name: iot_device_log_0_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_0_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202604_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_0_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202604_pkey;


--
-- Name: iot_device_log_0_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_0_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202605_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_0_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202605_pkey;


--
-- Name: iot_device_log_0_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_0_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202606_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_0_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202606_pkey;


--
-- Name: iot_device_log_0_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_0_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202607_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_0_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202607_pkey;


--
-- Name: iot_device_log_0_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_0_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202608_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_0_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202608_pkey;


--
-- Name: iot_device_log_0_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_0_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202609_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_0_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202609_pkey;


--
-- Name: iot_device_log_0_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_0_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_10_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_10_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202603_pkey;


--
-- Name: iot_device_log_10_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_10_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_10_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202604_pkey;


--
-- Name: iot_device_log_10_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_10_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_10_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202605_pkey;


--
-- Name: iot_device_log_10_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_10_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_10_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202606_pkey;


--
-- Name: iot_device_log_10_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_10_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_10_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202607_pkey;


--
-- Name: iot_device_log_10_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_10_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_10_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202608_pkey;


--
-- Name: iot_device_log_10_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_10_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_10_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202609_pkey;


--
-- Name: iot_device_log_10_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_10_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_11_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_11_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202603_pkey;


--
-- Name: iot_device_log_11_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_11_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_11_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202604_pkey;


--
-- Name: iot_device_log_11_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_11_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_11_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202605_pkey;


--
-- Name: iot_device_log_11_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_11_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_11_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202606_pkey;


--
-- Name: iot_device_log_11_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_11_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_11_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202607_pkey;


--
-- Name: iot_device_log_11_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_11_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_11_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202608_pkey;


--
-- Name: iot_device_log_11_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_11_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_11_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202609_pkey;


--
-- Name: iot_device_log_11_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_11_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_12_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_12_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202603_pkey;


--
-- Name: iot_device_log_12_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_12_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_12_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202604_pkey;


--
-- Name: iot_device_log_12_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_12_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_12_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202605_pkey;


--
-- Name: iot_device_log_12_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_12_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_12_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202606_pkey;


--
-- Name: iot_device_log_12_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_12_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_12_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202607_pkey;


--
-- Name: iot_device_log_12_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_12_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_12_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202608_pkey;


--
-- Name: iot_device_log_12_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_12_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_12_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202609_pkey;


--
-- Name: iot_device_log_12_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_12_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_13_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_13_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202603_pkey;


--
-- Name: iot_device_log_13_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_13_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_13_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202604_pkey;


--
-- Name: iot_device_log_13_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_13_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_13_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202605_pkey;


--
-- Name: iot_device_log_13_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_13_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_13_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202606_pkey;


--
-- Name: iot_device_log_13_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_13_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_13_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202607_pkey;


--
-- Name: iot_device_log_13_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_13_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_13_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202608_pkey;


--
-- Name: iot_device_log_13_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_13_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_13_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202609_pkey;


--
-- Name: iot_device_log_13_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_13_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_14_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_14_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202603_pkey;


--
-- Name: iot_device_log_14_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_14_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_14_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202604_pkey;


--
-- Name: iot_device_log_14_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_14_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_14_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202605_pkey;


--
-- Name: iot_device_log_14_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_14_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_14_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202606_pkey;


--
-- Name: iot_device_log_14_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_14_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_14_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202607_pkey;


--
-- Name: iot_device_log_14_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_14_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_14_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202608_pkey;


--
-- Name: iot_device_log_14_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_14_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_14_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202609_pkey;


--
-- Name: iot_device_log_14_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_14_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_15_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_15_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202603_pkey;


--
-- Name: iot_device_log_15_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_15_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_15_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202604_pkey;


--
-- Name: iot_device_log_15_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_15_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_15_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202605_pkey;


--
-- Name: iot_device_log_15_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_15_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_15_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202606_pkey;


--
-- Name: iot_device_log_15_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_15_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_15_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202607_pkey;


--
-- Name: iot_device_log_15_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_15_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_15_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202608_pkey;


--
-- Name: iot_device_log_15_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_15_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_15_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202609_pkey;


--
-- Name: iot_device_log_15_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_15_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_16_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_16_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202603_pkey;


--
-- Name: iot_device_log_16_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_16_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_16_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202604_pkey;


--
-- Name: iot_device_log_16_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_16_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_16_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202605_pkey;


--
-- Name: iot_device_log_16_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_16_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_16_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202606_pkey;


--
-- Name: iot_device_log_16_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_16_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_16_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202607_pkey;


--
-- Name: iot_device_log_16_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_16_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_16_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202608_pkey;


--
-- Name: iot_device_log_16_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_16_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_16_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202609_pkey;


--
-- Name: iot_device_log_16_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_16_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_17_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_17_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202603_pkey;


--
-- Name: iot_device_log_17_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_17_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_17_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202604_pkey;


--
-- Name: iot_device_log_17_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_17_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_17_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202605_pkey;


--
-- Name: iot_device_log_17_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_17_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_17_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202606_pkey;


--
-- Name: iot_device_log_17_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_17_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_17_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202607_pkey;


--
-- Name: iot_device_log_17_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_17_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_17_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202608_pkey;


--
-- Name: iot_device_log_17_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_17_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_17_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202609_pkey;


--
-- Name: iot_device_log_17_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_17_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_18_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_18_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202603_pkey;


--
-- Name: iot_device_log_18_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_18_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_18_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202604_pkey;


--
-- Name: iot_device_log_18_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_18_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_18_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202605_pkey;


--
-- Name: iot_device_log_18_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_18_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_18_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202606_pkey;


--
-- Name: iot_device_log_18_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_18_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_18_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202607_pkey;


--
-- Name: iot_device_log_18_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_18_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_18_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202608_pkey;


--
-- Name: iot_device_log_18_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_18_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_18_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202609_pkey;


--
-- Name: iot_device_log_18_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_18_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_19_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_19_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202603_pkey;


--
-- Name: iot_device_log_19_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_19_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_19_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202604_pkey;


--
-- Name: iot_device_log_19_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_19_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_19_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202605_pkey;


--
-- Name: iot_device_log_19_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_19_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_19_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202606_pkey;


--
-- Name: iot_device_log_19_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_19_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_19_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202607_pkey;


--
-- Name: iot_device_log_19_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_19_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_19_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202608_pkey;


--
-- Name: iot_device_log_19_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_19_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_19_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202609_pkey;


--
-- Name: iot_device_log_19_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_19_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_1_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202603_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_1_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202603_pkey;


--
-- Name: iot_device_log_1_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_1_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202604_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_1_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202604_pkey;


--
-- Name: iot_device_log_1_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_1_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202605_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_1_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202605_pkey;


--
-- Name: iot_device_log_1_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_1_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202606_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_1_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202606_pkey;


--
-- Name: iot_device_log_1_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_1_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202607_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_1_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202607_pkey;


--
-- Name: iot_device_log_1_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_1_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202608_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_1_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202608_pkey;


--
-- Name: iot_device_log_1_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_1_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202609_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_1_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202609_pkey;


--
-- Name: iot_device_log_1_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_1_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_20_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_20_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202603_pkey;


--
-- Name: iot_device_log_20_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_20_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_20_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202604_pkey;


--
-- Name: iot_device_log_20_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_20_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_20_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202605_pkey;


--
-- Name: iot_device_log_20_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_20_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_20_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202606_pkey;


--
-- Name: iot_device_log_20_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_20_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_20_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202607_pkey;


--
-- Name: iot_device_log_20_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_20_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_20_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202608_pkey;


--
-- Name: iot_device_log_20_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_20_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_20_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202609_pkey;


--
-- Name: iot_device_log_20_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_20_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_21_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_21_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202603_pkey;


--
-- Name: iot_device_log_21_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_21_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_21_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202604_pkey;


--
-- Name: iot_device_log_21_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_21_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_21_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202605_pkey;


--
-- Name: iot_device_log_21_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_21_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_21_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202606_pkey;


--
-- Name: iot_device_log_21_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_21_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_21_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202607_pkey;


--
-- Name: iot_device_log_21_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_21_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_21_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202608_pkey;


--
-- Name: iot_device_log_21_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_21_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_21_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202609_pkey;


--
-- Name: iot_device_log_21_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_21_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_22_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_22_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202603_pkey;


--
-- Name: iot_device_log_22_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_22_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_22_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202604_pkey;


--
-- Name: iot_device_log_22_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_22_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_22_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202605_pkey;


--
-- Name: iot_device_log_22_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_22_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_22_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202606_pkey;


--
-- Name: iot_device_log_22_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_22_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_22_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202607_pkey;


--
-- Name: iot_device_log_22_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_22_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_22_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202608_pkey;


--
-- Name: iot_device_log_22_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_22_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_22_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202609_pkey;


--
-- Name: iot_device_log_22_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_22_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_23_p202603_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202603_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_23_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202603_pkey;


--
-- Name: iot_device_log_23_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_23_p202604_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202604_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_23_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202604_pkey;


--
-- Name: iot_device_log_23_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_23_p202605_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202605_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_23_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202605_pkey;


--
-- Name: iot_device_log_23_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_23_p202606_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202606_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_23_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202606_pkey;


--
-- Name: iot_device_log_23_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_23_p202607_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202607_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_23_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202607_pkey;


--
-- Name: iot_device_log_23_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_23_p202608_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202608_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_23_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202608_pkey;


--
-- Name: iot_device_log_23_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_23_p202609_iot_id_message_type_create_time_e_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202609_iot_id_message_type_create_time_e_idx;


--
-- Name: iot_device_log_23_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202609_pkey;


--
-- Name: iot_device_log_23_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_23_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_2_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202603_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_2_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202603_pkey;


--
-- Name: iot_device_log_2_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_2_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202604_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_2_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202604_pkey;


--
-- Name: iot_device_log_2_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_2_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202605_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_2_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202605_pkey;


--
-- Name: iot_device_log_2_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_2_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202606_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_2_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202606_pkey;


--
-- Name: iot_device_log_2_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_2_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202607_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_2_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202607_pkey;


--
-- Name: iot_device_log_2_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_2_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202608_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_2_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202608_pkey;


--
-- Name: iot_device_log_2_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_2_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202609_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_2_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202609_pkey;


--
-- Name: iot_device_log_2_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_2_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_3_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202603_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_3_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202603_pkey;


--
-- Name: iot_device_log_3_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_3_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202604_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_3_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202604_pkey;


--
-- Name: iot_device_log_3_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_3_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202605_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_3_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202605_pkey;


--
-- Name: iot_device_log_3_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_3_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202606_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_3_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202606_pkey;


--
-- Name: iot_device_log_3_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_3_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202607_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_3_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202607_pkey;


--
-- Name: iot_device_log_3_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_3_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202608_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_3_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202608_pkey;


--
-- Name: iot_device_log_3_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_3_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202609_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_3_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202609_pkey;


--
-- Name: iot_device_log_3_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_3_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_4_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202603_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_4_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202603_pkey;


--
-- Name: iot_device_log_4_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_4_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202604_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_4_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202604_pkey;


--
-- Name: iot_device_log_4_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_4_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202605_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_4_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202605_pkey;


--
-- Name: iot_device_log_4_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_4_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202606_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_4_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202606_pkey;


--
-- Name: iot_device_log_4_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_4_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202607_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_4_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202607_pkey;


--
-- Name: iot_device_log_4_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_4_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202608_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_4_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202608_pkey;


--
-- Name: iot_device_log_4_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_4_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202609_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_4_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202609_pkey;


--
-- Name: iot_device_log_4_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_4_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_5_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202603_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_5_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202603_pkey;


--
-- Name: iot_device_log_5_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_5_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202604_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_5_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202604_pkey;


--
-- Name: iot_device_log_5_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_5_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202605_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_5_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202605_pkey;


--
-- Name: iot_device_log_5_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_5_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202606_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_5_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202606_pkey;


--
-- Name: iot_device_log_5_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_5_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202607_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_5_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202607_pkey;


--
-- Name: iot_device_log_5_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_5_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202608_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_5_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202608_pkey;


--
-- Name: iot_device_log_5_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_5_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202609_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_5_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202609_pkey;


--
-- Name: iot_device_log_5_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_5_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_6_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202603_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_6_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202603_pkey;


--
-- Name: iot_device_log_6_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_6_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202604_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_6_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202604_pkey;


--
-- Name: iot_device_log_6_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_6_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202605_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_6_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202605_pkey;


--
-- Name: iot_device_log_6_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_6_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202606_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_6_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202606_pkey;


--
-- Name: iot_device_log_6_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_6_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202607_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_6_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202607_pkey;


--
-- Name: iot_device_log_6_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_6_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202608_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_6_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202608_pkey;


--
-- Name: iot_device_log_6_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_6_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202609_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_6_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202609_pkey;


--
-- Name: iot_device_log_6_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_6_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_7_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202603_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_7_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202603_pkey;


--
-- Name: iot_device_log_7_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_7_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202604_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_7_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202604_pkey;


--
-- Name: iot_device_log_7_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_7_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202605_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_7_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202605_pkey;


--
-- Name: iot_device_log_7_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_7_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202606_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_7_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202606_pkey;


--
-- Name: iot_device_log_7_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_7_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202607_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_7_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202607_pkey;


--
-- Name: iot_device_log_7_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_7_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202608_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_7_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202608_pkey;


--
-- Name: iot_device_log_7_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_7_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202609_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_7_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202609_pkey;


--
-- Name: iot_device_log_7_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_7_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_8_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202603_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_8_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202603_pkey;


--
-- Name: iot_device_log_8_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_8_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202604_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_8_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202604_pkey;


--
-- Name: iot_device_log_8_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_8_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202605_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_8_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202605_pkey;


--
-- Name: iot_device_log_8_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_8_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202606_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_8_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202606_pkey;


--
-- Name: iot_device_log_8_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_8_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202607_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_8_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202607_pkey;


--
-- Name: iot_device_log_8_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_8_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202608_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_8_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202608_pkey;


--
-- Name: iot_device_log_8_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_8_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202609_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_8_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202609_pkey;


--
-- Name: iot_device_log_8_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_8_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_9_p202603_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202603_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_9_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202603_pkey;


--
-- Name: iot_device_log_9_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_9_p202604_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202604_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_9_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202604_pkey;


--
-- Name: iot_device_log_9_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_9_p202605_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202605_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_9_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202605_pkey;


--
-- Name: iot_device_log_9_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_9_p202606_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202606_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_9_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202606_pkey;


--
-- Name: iot_device_log_9_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_9_p202607_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202607_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_9_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202607_pkey;


--
-- Name: iot_device_log_9_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_9_p202608_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202608_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_9_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202608_pkey;


--
-- Name: iot_device_log_9_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_9_p202609_iot_id_message_type_create_time_ev_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_log_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202609_iot_id_message_type_create_time_ev_idx;


--
-- Name: iot_device_log_9_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202609_pkey;


--
-- Name: iot_device_log_9_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_9_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_0_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202603_pkey;


--
-- Name: iot_device_log_metadata_0_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_0_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202604_pkey;


--
-- Name: iot_device_log_metadata_0_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_0_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202605_pkey;


--
-- Name: iot_device_log_metadata_0_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_0_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202606_pkey;


--
-- Name: iot_device_log_metadata_0_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_0_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202607_pkey;


--
-- Name: iot_device_log_metadata_0_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_0_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202608_pkey;


--
-- Name: iot_device_log_metadata_0_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_0_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_0_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202609_pkey;


--
-- Name: iot_device_log_metadata_0_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_0_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p20_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p20_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p20_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p20_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p20_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p20_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_0_p20_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_0 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_0_p20_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_10_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202603_pkey;


--
-- Name: iot_device_log_metadata_10_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_10_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202604_pkey;


--
-- Name: iot_device_log_metadata_10_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_10_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202605_pkey;


--
-- Name: iot_device_log_metadata_10_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_10_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202606_pkey;


--
-- Name: iot_device_log_metadata_10_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_10_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202607_pkey;


--
-- Name: iot_device_log_metadata_10_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_10_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202608_pkey;


--
-- Name: iot_device_log_metadata_10_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_10_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_10_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202609_pkey;


--
-- Name: iot_device_log_metadata_10_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_10_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_10_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_10 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_10_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_11_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202603_pkey;


--
-- Name: iot_device_log_metadata_11_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_11_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202604_pkey;


--
-- Name: iot_device_log_metadata_11_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_11_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202605_pkey;


--
-- Name: iot_device_log_metadata_11_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_11_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202606_pkey;


--
-- Name: iot_device_log_metadata_11_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_11_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202607_pkey;


--
-- Name: iot_device_log_metadata_11_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_11_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202608_pkey;


--
-- Name: iot_device_log_metadata_11_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_11_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_11_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202609_pkey;


--
-- Name: iot_device_log_metadata_11_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_11_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_11_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_11 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_11_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_12_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202603_pkey;


--
-- Name: iot_device_log_metadata_12_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_12_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202604_pkey;


--
-- Name: iot_device_log_metadata_12_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_12_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202605_pkey;


--
-- Name: iot_device_log_metadata_12_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_12_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202606_pkey;


--
-- Name: iot_device_log_metadata_12_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_12_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202607_pkey;


--
-- Name: iot_device_log_metadata_12_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_12_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202608_pkey;


--
-- Name: iot_device_log_metadata_12_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_12_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_12_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202609_pkey;


--
-- Name: iot_device_log_metadata_12_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_12_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_12_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_12 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_12_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_13_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202603_pkey;


--
-- Name: iot_device_log_metadata_13_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_13_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202604_pkey;


--
-- Name: iot_device_log_metadata_13_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_13_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202605_pkey;


--
-- Name: iot_device_log_metadata_13_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_13_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202606_pkey;


--
-- Name: iot_device_log_metadata_13_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_13_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202607_pkey;


--
-- Name: iot_device_log_metadata_13_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_13_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202608_pkey;


--
-- Name: iot_device_log_metadata_13_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_13_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_13_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202609_pkey;


--
-- Name: iot_device_log_metadata_13_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_13_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_13_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_13 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_13_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_14_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202603_pkey;


--
-- Name: iot_device_log_metadata_14_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_14_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202604_pkey;


--
-- Name: iot_device_log_metadata_14_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_14_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202605_pkey;


--
-- Name: iot_device_log_metadata_14_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_14_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202606_pkey;


--
-- Name: iot_device_log_metadata_14_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_14_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202607_pkey;


--
-- Name: iot_device_log_metadata_14_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_14_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202608_pkey;


--
-- Name: iot_device_log_metadata_14_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_14_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_14_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202609_pkey;


--
-- Name: iot_device_log_metadata_14_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_14_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_14_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_14 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_14_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_15_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202603_pkey;


--
-- Name: iot_device_log_metadata_15_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_15_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202604_pkey;


--
-- Name: iot_device_log_metadata_15_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_15_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202605_pkey;


--
-- Name: iot_device_log_metadata_15_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_15_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202606_pkey;


--
-- Name: iot_device_log_metadata_15_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_15_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202607_pkey;


--
-- Name: iot_device_log_metadata_15_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_15_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202608_pkey;


--
-- Name: iot_device_log_metadata_15_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_15_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_15_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202609_pkey;


--
-- Name: iot_device_log_metadata_15_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_15_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_15_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_15 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_15_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_16_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202603_pkey;


--
-- Name: iot_device_log_metadata_16_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_16_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202604_pkey;


--
-- Name: iot_device_log_metadata_16_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_16_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202605_pkey;


--
-- Name: iot_device_log_metadata_16_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_16_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202606_pkey;


--
-- Name: iot_device_log_metadata_16_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_16_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202607_pkey;


--
-- Name: iot_device_log_metadata_16_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_16_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202608_pkey;


--
-- Name: iot_device_log_metadata_16_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_16_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_16_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202609_pkey;


--
-- Name: iot_device_log_metadata_16_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_16_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_16_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_16 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_16_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_17_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202603_pkey;


--
-- Name: iot_device_log_metadata_17_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_17_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202604_pkey;


--
-- Name: iot_device_log_metadata_17_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_17_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202605_pkey;


--
-- Name: iot_device_log_metadata_17_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_17_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202606_pkey;


--
-- Name: iot_device_log_metadata_17_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_17_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202607_pkey;


--
-- Name: iot_device_log_metadata_17_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_17_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202608_pkey;


--
-- Name: iot_device_log_metadata_17_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_17_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_17_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202609_pkey;


--
-- Name: iot_device_log_metadata_17_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_17_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_17_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_17 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_17_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_18_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202603_pkey;


--
-- Name: iot_device_log_metadata_18_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_18_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202604_pkey;


--
-- Name: iot_device_log_metadata_18_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_18_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202605_pkey;


--
-- Name: iot_device_log_metadata_18_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_18_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202606_pkey;


--
-- Name: iot_device_log_metadata_18_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_18_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202607_pkey;


--
-- Name: iot_device_log_metadata_18_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_18_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202608_pkey;


--
-- Name: iot_device_log_metadata_18_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_18_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_18_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202609_pkey;


--
-- Name: iot_device_log_metadata_18_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_18_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_18_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_18 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_18_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_19_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202603_pkey;


--
-- Name: iot_device_log_metadata_19_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_19_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202604_pkey;


--
-- Name: iot_device_log_metadata_19_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_19_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202605_pkey;


--
-- Name: iot_device_log_metadata_19_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_19_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202606_pkey;


--
-- Name: iot_device_log_metadata_19_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_19_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202607_pkey;


--
-- Name: iot_device_log_metadata_19_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_19_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202608_pkey;


--
-- Name: iot_device_log_metadata_19_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_19_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_19_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202609_pkey;


--
-- Name: iot_device_log_metadata_19_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_19_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_19_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_19 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_19_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_1_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202603_pkey;


--
-- Name: iot_device_log_metadata_1_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_1_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202604_pkey;


--
-- Name: iot_device_log_metadata_1_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_1_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202605_pkey;


--
-- Name: iot_device_log_metadata_1_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_1_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202606_pkey;


--
-- Name: iot_device_log_metadata_1_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_1_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202607_pkey;


--
-- Name: iot_device_log_metadata_1_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_1_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202608_pkey;


--
-- Name: iot_device_log_metadata_1_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_1_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_1_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202609_pkey;


--
-- Name: iot_device_log_metadata_1_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_1_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p20_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p20_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p20_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p20_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p20_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p20_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_1_p20_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_1 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_1_p20_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_20_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202603_pkey;


--
-- Name: iot_device_log_metadata_20_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_20_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202604_pkey;


--
-- Name: iot_device_log_metadata_20_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_20_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202605_pkey;


--
-- Name: iot_device_log_metadata_20_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_20_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202606_pkey;


--
-- Name: iot_device_log_metadata_20_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_20_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202607_pkey;


--
-- Name: iot_device_log_metadata_20_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_20_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202608_pkey;


--
-- Name: iot_device_log_metadata_20_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_20_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_20_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202609_pkey;


--
-- Name: iot_device_log_metadata_20_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_20_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_20_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_20 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_20_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_21_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202603_pkey;


--
-- Name: iot_device_log_metadata_21_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_21_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202604_pkey;


--
-- Name: iot_device_log_metadata_21_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_21_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202605_pkey;


--
-- Name: iot_device_log_metadata_21_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_21_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202606_pkey;


--
-- Name: iot_device_log_metadata_21_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_21_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202607_pkey;


--
-- Name: iot_device_log_metadata_21_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_21_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202608_pkey;


--
-- Name: iot_device_log_metadata_21_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_21_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_21_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202609_pkey;


--
-- Name: iot_device_log_metadata_21_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_21_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_21_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_21 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_21_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_22_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202603_pkey;


--
-- Name: iot_device_log_metadata_22_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_22_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202604_pkey;


--
-- Name: iot_device_log_metadata_22_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_22_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202605_pkey;


--
-- Name: iot_device_log_metadata_22_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_22_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202606_pkey;


--
-- Name: iot_device_log_metadata_22_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_22_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202607_pkey;


--
-- Name: iot_device_log_metadata_22_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_22_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202608_pkey;


--
-- Name: iot_device_log_metadata_22_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_22_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_22_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202609_pkey;


--
-- Name: iot_device_log_metadata_22_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_22_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_22_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_22 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_22_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_23_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202603_pkey;


--
-- Name: iot_device_log_metadata_23_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_23_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202604_pkey;


--
-- Name: iot_device_log_metadata_23_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_23_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202605_pkey;


--
-- Name: iot_device_log_metadata_23_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_23_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202606_pkey;


--
-- Name: iot_device_log_metadata_23_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_23_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202607_pkey;


--
-- Name: iot_device_log_metadata_23_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_23_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202608_pkey;


--
-- Name: iot_device_log_metadata_23_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_23_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_23_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202609_pkey;


--
-- Name: iot_device_log_metadata_23_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_23_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p2_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p2_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p2_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p2_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p2_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p2_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_23_p2_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_23 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_23_p2_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_2_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202603_pkey;


--
-- Name: iot_device_log_metadata_2_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_2_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202604_pkey;


--
-- Name: iot_device_log_metadata_2_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_2_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202605_pkey;


--
-- Name: iot_device_log_metadata_2_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_2_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202606_pkey;


--
-- Name: iot_device_log_metadata_2_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_2_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202607_pkey;


--
-- Name: iot_device_log_metadata_2_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_2_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202608_pkey;


--
-- Name: iot_device_log_metadata_2_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_2_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_2_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202609_pkey;


--
-- Name: iot_device_log_metadata_2_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_2_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p20_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p20_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p20_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p20_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p20_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p20_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_2_p20_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_2 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_2_p20_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_3_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202603_pkey;


--
-- Name: iot_device_log_metadata_3_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_3_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202604_pkey;


--
-- Name: iot_device_log_metadata_3_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_3_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202605_pkey;


--
-- Name: iot_device_log_metadata_3_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_3_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202606_pkey;


--
-- Name: iot_device_log_metadata_3_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_3_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202607_pkey;


--
-- Name: iot_device_log_metadata_3_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_3_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202608_pkey;


--
-- Name: iot_device_log_metadata_3_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_3_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_3_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202609_pkey;


--
-- Name: iot_device_log_metadata_3_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_3_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p20_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p20_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p20_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p20_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p20_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p20_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_3_p20_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_3 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_3_p20_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_4_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202603_pkey;


--
-- Name: iot_device_log_metadata_4_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_4_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202604_pkey;


--
-- Name: iot_device_log_metadata_4_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_4_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202605_pkey;


--
-- Name: iot_device_log_metadata_4_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_4_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202606_pkey;


--
-- Name: iot_device_log_metadata_4_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_4_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202607_pkey;


--
-- Name: iot_device_log_metadata_4_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_4_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202608_pkey;


--
-- Name: iot_device_log_metadata_4_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_4_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_4_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202609_pkey;


--
-- Name: iot_device_log_metadata_4_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_4_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p20_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p20_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p20_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p20_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p20_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p20_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_4_p20_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_4 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_4_p20_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_5_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202603_pkey;


--
-- Name: iot_device_log_metadata_5_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_5_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202604_pkey;


--
-- Name: iot_device_log_metadata_5_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_5_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202605_pkey;


--
-- Name: iot_device_log_metadata_5_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_5_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202606_pkey;


--
-- Name: iot_device_log_metadata_5_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_5_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202607_pkey;


--
-- Name: iot_device_log_metadata_5_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_5_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202608_pkey;


--
-- Name: iot_device_log_metadata_5_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_5_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_5_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202609_pkey;


--
-- Name: iot_device_log_metadata_5_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_5_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p20_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p20_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p20_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p20_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p20_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p20_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_5_p20_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_5 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_5_p20_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_6_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202603_pkey;


--
-- Name: iot_device_log_metadata_6_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_6_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202604_pkey;


--
-- Name: iot_device_log_metadata_6_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_6_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202605_pkey;


--
-- Name: iot_device_log_metadata_6_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_6_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202606_pkey;


--
-- Name: iot_device_log_metadata_6_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_6_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202607_pkey;


--
-- Name: iot_device_log_metadata_6_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_6_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202608_pkey;


--
-- Name: iot_device_log_metadata_6_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_6_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_6_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202609_pkey;


--
-- Name: iot_device_log_metadata_6_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_6_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p20_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p20_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p20_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p20_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p20_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p20_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_6_p20_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_6 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_6_p20_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_7_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202603_pkey;


--
-- Name: iot_device_log_metadata_7_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_7_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202604_pkey;


--
-- Name: iot_device_log_metadata_7_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_7_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202605_pkey;


--
-- Name: iot_device_log_metadata_7_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_7_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202606_pkey;


--
-- Name: iot_device_log_metadata_7_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_7_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202607_pkey;


--
-- Name: iot_device_log_metadata_7_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_7_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202608_pkey;


--
-- Name: iot_device_log_metadata_7_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_7_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_7_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202609_pkey;


--
-- Name: iot_device_log_metadata_7_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_7_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p20_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p20_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p20_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p20_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p20_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p20_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_7_p20_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_7 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_7_p20_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_8_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202603_pkey;


--
-- Name: iot_device_log_metadata_8_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_8_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202604_pkey;


--
-- Name: iot_device_log_metadata_8_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_8_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202605_pkey;


--
-- Name: iot_device_log_metadata_8_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_8_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202606_pkey;


--
-- Name: iot_device_log_metadata_8_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_8_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202607_pkey;


--
-- Name: iot_device_log_metadata_8_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_8_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202608_pkey;


--
-- Name: iot_device_log_metadata_8_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_8_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_8_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202609_pkey;


--
-- Name: iot_device_log_metadata_8_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_8_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p20_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p20_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p20_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p20_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p20_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p20_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_8_p20_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_8 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_8_p20_iot_id_message_type_property_idx6;


--
-- Name: iot_device_log_metadata_9_p202603_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202603_pkey;


--
-- Name: iot_device_log_metadata_9_p202603_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202603_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_9_p202604_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202604_pkey;


--
-- Name: iot_device_log_metadata_9_p202604_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202604_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_9_p202605_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202605_pkey;


--
-- Name: iot_device_log_metadata_9_p202605_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202605_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_9_p202606_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202606_pkey;


--
-- Name: iot_device_log_metadata_9_p202606_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202606_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_9_p202607_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202607_pkey;


--
-- Name: iot_device_log_metadata_9_p202607_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202607_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_9_p202608_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202608_pkey;


--
-- Name: iot_device_log_metadata_9_p202608_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202608_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_9_p202609_pkey; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.iot_device_log_metadata_9_pkey ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202609_pkey;


--
-- Name: iot_device_log_metadata_9_p202609_tenant_id_create_time_idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_iot_device_log_metadata_9_tenant_time ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p202609_tenant_id_create_time_idx;


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property__idx; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p20_iot_id_message_type_property__idx;


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property_idx1; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p20_iot_id_message_type_property_idx1;


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property_idx2; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p20_iot_id_message_type_property_idx2;


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property_idx3; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p20_iot_id_message_type_property_idx3;


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property_idx4; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p20_iot_id_message_type_property_idx4;


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property_idx5; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p20_iot_id_message_type_property_idx5;


--
-- Name: iot_device_log_metadata_9_p20_iot_id_message_type_property_idx6; Type: INDEX ATTACH; Schema: unix_tenant_202605; Owner: -
--

ALTER INDEX unix_tenant_202605.idx_meta_core_9 ATTACH PARTITION unix_tenant_202605.iot_device_log_metadata_9_p20_iot_id_message_type_property_idx6;


--
-- Name: iot_algorithm_alert_record trg_iot_algorithm_alert_record_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_algorithm_alert_record_update_time BEFORE UPDATE ON unix_tenant_202605.iot_algorithm_alert_record FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_algorithm_config trg_iot_algorithm_config_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_algorithm_config_update_time BEFORE UPDATE ON unix_tenant_202605.iot_algorithm_config FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_certificate trg_iot_certificate_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_certificate_update_time BEFORE UPDATE ON unix_tenant_202605.iot_certificate FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_dashboard_statistics trg_iot_dashboard_statistics_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_dashboard_statistics_update_time BEFORE UPDATE ON unix_tenant_202605.iot_dashboard_statistics FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_data_bridge_config trg_iot_data_bridge_config_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_data_bridge_config_update_time BEFORE UPDATE ON unix_tenant_202605.iot_data_bridge_config FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_data_input_task trg_iot_data_input_task_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_data_input_task_update_time BEFORE UPDATE ON unix_tenant_202605.iot_data_input_task FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_device_group trg_iot_device_group_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_device_group_update_time BEFORE UPDATE ON unix_tenant_202605.iot_device_group FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_device_model trg_iot_device_model_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_device_model_update_time BEFORE UPDATE ON unix_tenant_202605.iot_device_model FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_device_shadow trg_iot_device_shadow_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_device_shadow_update_time BEFORE UPDATE ON unix_tenant_202605.iot_device_shadow FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_device_statistics trg_iot_device_statistics_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_device_statistics_update_time BEFORE UPDATE ON unix_tenant_202605.iot_device_statistics FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_device_up_format trg_iot_device_up_format_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_device_up_format_update_time BEFORE UPDATE ON unix_tenant_202605.iot_device_up_format FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_gateway_polling_command trg_iot_gateway_polling_command_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_gateway_polling_command_update_time BEFORE UPDATE ON unix_tenant_202605.iot_gateway_polling_command FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_gateway_polling_config trg_iot_gateway_polling_config_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_gateway_polling_config_update_time BEFORE UPDATE ON unix_tenant_202605.iot_gateway_polling_config FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_ota_device_version trg_iot_ota_device_version_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_ota_device_version_update_time BEFORE UPDATE ON unix_tenant_202605.iot_ota_device_version FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_ota_package trg_iot_ota_package_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_ota_package_update_time BEFORE UPDATE ON unix_tenant_202605.iot_ota_package FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_ota_tag trg_iot_ota_tag_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_ota_tag_update_time BEFORE UPDATE ON unix_tenant_202605.iot_ota_tag FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_ota_task_device trg_iot_ota_task_device_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_ota_task_device_update_time BEFORE UPDATE ON unix_tenant_202605.iot_ota_task_device FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_ota_task trg_iot_ota_task_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_ota_task_update_time BEFORE UPDATE ON unix_tenant_202605.iot_ota_task FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_product trg_iot_product_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_product_update_time BEFORE UPDATE ON unix_tenant_202605.iot_product FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_push_transform_rule trg_iot_push_transform_rule_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_push_transform_rule_update_time BEFORE UPDATE ON unix_tenant_202605.iot_push_transform_rule FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_resource_connection trg_iot_resource_connection_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_resource_connection_update_time BEFORE UPDATE ON unix_tenant_202605.iot_resource_connection FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_standard_category trg_iot_standard_category_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_standard_category_update_time BEFORE UPDATE ON unix_tenant_202605.iot_standard_category FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_standard_metadata_field trg_iot_standard_metadata_field_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_standard_metadata_field_update_time BEFORE UPDATE ON unix_tenant_202605.iot_standard_metadata_field FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_standard_product trg_iot_standard_product_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_iot_standard_product_update_time BEFORE UPDATE ON unix_tenant_202605.iot_standard_product FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: mp_app trg_mp_app_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_mp_app_update_time BEFORE UPDATE ON unix_tenant_202605.mp_app FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: mp_component_library trg_mp_component_library_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_mp_component_library_update_time BEFORE UPDATE ON unix_tenant_202605.mp_component_library FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: mp_detail_layout trg_mp_detail_layout_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_mp_detail_layout_update_time BEFORE UPDATE ON unix_tenant_202605.mp_detail_layout FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: mp_diy_page trg_mp_diy_page_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_mp_diy_page_update_time BEFORE UPDATE ON unix_tenant_202605.mp_diy_page FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: mp_diy_template trg_mp_diy_template_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_mp_diy_template_update_time BEFORE UPDATE ON unix_tenant_202605.mp_diy_template FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: mp_home_component trg_mp_home_component_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_mp_home_component_update_time BEFORE UPDATE ON unix_tenant_202605.mp_home_component FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: mp_home_layout trg_mp_home_layout_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_mp_home_layout_update_time BEFORE UPDATE ON unix_tenant_202605.mp_home_layout FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: mp_layout_template trg_mp_layout_template_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_mp_layout_template_update_time BEFORE UPDATE ON unix_tenant_202605.mp_layout_template FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: mp_user trg_mp_user_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_mp_user_update_time BEFORE UPDATE ON unix_tenant_202605.mp_user FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: notice_channel trg_notice_channel_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_notice_channel_update_time BEFORE UPDATE ON unix_tenant_202605.notice_channel FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: notice_template trg_notice_template_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_notice_template_update_time BEFORE UPDATE ON unix_tenant_202605.notice_template FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: rulego_chain trg_rulego_chain_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_rulego_chain_update_time BEFORE UPDATE ON unix_tenant_202605.rulego_chain FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: video_platform_channel trg_video_platform_channel_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_video_platform_channel_update_time BEFORE UPDATE ON unix_tenant_202605.video_platform_channel FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: video_platform_device_cache trg_video_platform_device_cache_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_video_platform_device_cache_update_time BEFORE UPDATE ON unix_tenant_202605.video_platform_device_cache FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: video_platform_device_ext trg_video_platform_device_ext_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_video_platform_device_ext_update_time BEFORE UPDATE ON unix_tenant_202605.video_platform_device_ext FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: video_platform_device trg_video_platform_device_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_video_platform_device_update_time BEFORE UPDATE ON unix_tenant_202605.video_platform_device FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: video_platform_instance trg_video_platform_instance_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_video_platform_instance_update_time BEFORE UPDATE ON unix_tenant_202605.video_platform_instance FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: video_platform_org_cache trg_video_platform_org_cache_update_time; Type: TRIGGER; Schema: unix_tenant_202605; Owner: -
--

CREATE TRIGGER trg_video_platform_org_cache_update_time BEFORE UPDATE ON unix_tenant_202605.video_platform_org_cache FOR EACH ROW EXECUTE FUNCTION unix_tenant_202605.update_timestamp();


--
-- Name: iot_industrial_asset fk_iot_ind_asset_endpoint; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_asset
    ADD CONSTRAINT fk_iot_ind_asset_endpoint FOREIGN KEY (endpoint_id) REFERENCES unix_tenant_202605.iot_industrial_endpoint(id) ON DELETE CASCADE;


--
-- Name: iot_industrial_collect_point fk_iot_ind_collect_point_group; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_collect_point
    ADD CONSTRAINT fk_iot_ind_collect_point_group FOREIGN KEY (group_id) REFERENCES unix_tenant_202605.iot_industrial_collect_group(id) ON DELETE CASCADE;


--
-- Name: iot_industrial_collect_point fk_iot_ind_collect_point_point; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_collect_point
    ADD CONSTRAINT fk_iot_ind_collect_point_point FOREIGN KEY (point_id) REFERENCES unix_tenant_202605.iot_industrial_point(id) ON DELETE CASCADE;


--
-- Name: iot_industrial_config_template_version fk_iot_ind_config_template_version_template; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_config_template_version
    ADD CONSTRAINT fk_iot_ind_config_template_version_template FOREIGN KEY (template_id) REFERENCES unix_tenant_202605.iot_industrial_config_template(id) ON DELETE CASCADE;


--
-- Name: iot_industrial_collect_group fk_iot_ind_group_endpoint; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_collect_group
    ADD CONSTRAINT fk_iot_ind_group_endpoint FOREIGN KEY (endpoint_id) REFERENCES unix_tenant_202605.iot_industrial_endpoint(id) ON DELETE CASCADE;


--
-- Name: iot_industrial_point fk_iot_ind_point_endpoint; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_point
    ADD CONSTRAINT fk_iot_ind_point_endpoint FOREIGN KEY (endpoint_id) REFERENCES unix_tenant_202605.iot_industrial_endpoint(id) ON DELETE CASCADE;


--
-- Name: iot_industrial_runtime_stat fk_iot_ind_stat_endpoint; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_runtime_stat
    ADD CONSTRAINT fk_iot_ind_stat_endpoint FOREIGN KEY (endpoint_id) REFERENCES unix_tenant_202605.iot_industrial_endpoint(id) ON DELETE CASCADE;


--
-- Name: iot_industrial_runtime_stat fk_iot_ind_stat_group; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.iot_industrial_runtime_stat
    ADD CONSTRAINT fk_iot_ind_stat_group FOREIGN KEY (group_id) REFERENCES unix_tenant_202605.iot_industrial_collect_group(id) ON DELETE CASCADE;


--
-- Name: qrtz_blob_triggers qrtz_blob_triggers_sched_name_trigger_name_trigger_group_fkey; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_sched_name_trigger_name_trigger_group_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES unix_tenant_202605.qrtz_triggers(sched_name, trigger_name, trigger_group) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: qrtz_cron_triggers qrtz_cron_triggers_sched_name_trigger_name_trigger_group_fkey; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_sched_name_trigger_name_trigger_group_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES unix_tenant_202605.qrtz_triggers(sched_name, trigger_name, trigger_group) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: qrtz_simple_triggers qrtz_simple_triggers_sched_name_trigger_name_trigger_group_fkey; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_sched_name_trigger_name_trigger_group_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES unix_tenant_202605.qrtz_triggers(sched_name, trigger_name, trigger_group) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: qrtz_simprop_triggers qrtz_simprop_triggers_sched_name_trigger_name_trigger_grou_fkey; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_simprop_triggers
    ADD CONSTRAINT qrtz_simprop_triggers_sched_name_trigger_name_trigger_grou_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES unix_tenant_202605.qrtz_triggers(sched_name, trigger_name, trigger_group) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: qrtz_triggers qrtz_triggers_sched_name_job_name_job_group_fkey; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_sched_name_job_name_job_group_fkey FOREIGN KEY (sched_name, job_name, job_group) REFERENCES unix_tenant_202605.qrtz_job_details(sched_name, job_name, job_group) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: video_platform_channel video_platform_channel_device_id_fkey; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_platform_channel
    ADD CONSTRAINT video_platform_channel_device_id_fkey FOREIGN KEY (device_id) REFERENCES unix_tenant_202605.video_platform_device(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- Name: video_platform_device_ext video_platform_device_ext_device_id_fkey; Type: FK CONSTRAINT; Schema: unix_tenant_202605; Owner: -
--

ALTER TABLE ONLY unix_tenant_202605.video_platform_device_ext
    ADD CONSTRAINT video_platform_device_ext_device_id_fkey FOREIGN KEY (device_id) REFERENCES unix_tenant_202605.video_platform_device(id) ON UPDATE RESTRICT ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--


-- Point templates share immutable version storage with legacy CONFIG templates.
ALTER TABLE ONLY unix_tenant_202605.iot_industrial_config_template
    ADD CONSTRAINT ck_iot_ind_config_template_type CHECK (template_type IN ('CONFIG', 'POINTS')),
    ADD CONSTRAINT ck_iot_ind_config_template_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    ADD CONSTRAINT ck_iot_ind_config_template_binding CHECK (template_type <> 'POINTS' OR (source_product_key IS NOT NULL AND btrim(source_product_key) <> '' AND protocol IS NOT NULL AND btrim(protocol) <> '')),
    ADD CONSTRAINT ck_iot_ind_config_template_revision CHECK (draft_revision >= 0 AND latest_version >= 0);

CREATE INDEX idx_iot_ind_config_template_point_filter
    ON unix_tenant_202605.iot_industrial_config_template USING btree (tenant_id, template_type, status, protocol, source_product_key);

-- ISUP durable callback identity and retry indexes.
ALTER TABLE unix_tenant_202605.video_sdk_alarm_receipt ADD CONSTRAINT video_sdk_alarm_receipt_pkey PRIMARY KEY (id);
CREATE UNIQUE INDEX uk_video_alarm_receipt_identity ON unix_tenant_202605.video_sdk_alarm_receipt (tenant_id,instance_key,device_id,channel_id,event_id,event_kind,image_fingerprint);
CREATE INDEX idx_video_alarm_receipt_due ON unix_tenant_202605.video_sdk_alarm_receipt (status,next_attempt_at,lease_until);
CREATE INDEX idx_video_alarm_receipt_cooldown ON unix_tenant_202605.video_sdk_alarm_receipt (tenant_id,instance_key,device_id,channel_id,event_kind,created_at);

-- Reviewed V2026101304 motion process query indexes.
CREATE INDEX idx_video_alarm_process_query ON unix_tenant_202605.video_sdk_alarm_process (tenant_id,instance_key,device_id,channel_id,row_kind,last_time);
CREATE INDEX idx_video_alarm_process_gap ON unix_tenant_202605.video_sdk_alarm_process (row_kind,state,last_received_at);

CREATE INDEX idx_video_alarm_member_page ON unix_tenant_202605.video_sdk_alarm_process_member (process_id,alarm_time,receipt_id);
CREATE INDEX idx_video_alarm_member_event ON unix_tenant_202605.video_sdk_alarm_process_member (tenant_id,instance_key,device_id,channel_id,event_id);
