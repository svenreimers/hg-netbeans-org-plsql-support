----------------------------------------------------------------
--
-- File     :  FNDBASDR.SQL
--
-- Component:  FNDBAS - IFS Base Functionality
--
-- Release  :
--
-- Created  : 2012-10-22
--
----------------------------------------------------------------
-- NOTE  This script drops the complete component and must be
-- NOTE  edited before usage.
-- NOTE  Number of synonyms, triggers, sequences, MVs and tables
-- NOTE  might not be exact due to incomplete information in the
-- NOTE  database
-- NOTE  Please check with CRE/INS script(s) to make sure that
-- NOTE  correct objects are dropped
-- NOTE  The script will drop
-- NOTE  -  Obvious component data stored in other component's frameworks
-- NOTE  - Database objects for the component
----------------------------------------------------------------

-- Remove the EXIT statement when the template script has been verified
-- Start removing/commenting here
PROMPT ****************************************************************
PROMPT NOTE NOTE NOTE NOTE NOTE NOTE NOTE NOTE NOTE NOTE NOTE NOTE NOTE
PROMPT ****************************************************************
PROMPT The script must be edited/verified first
PROMPT Execution will end here
EXIT
-- END removing/commenting here

ACCEPT remove_data DEFAULT 'No' FORMAT 'A3' PROMPT 'Do you want to remove data related to component as well [(Y)es/(N)o, The default is No]? '

SET SERVEROUTPUT ON SIZE UNLIMITED
SET VERIFY OFF
SET FEEDBACK OFF


SPOOL FNDBASDR.log

--=======================================================
-- DATA REMOVAL SECTION START
--=======================================================

BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      --=======================================================
      dbms_output.put_line('START removing some module specific data...');
      dbms_output.put_line('Please Wait...');
      --=======================================================
   END IF;
END;
/

--=======================================================
-- Removing Posting Controls and Related Data Start
--=======================================================

BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Posting Controls and Related Data...');
   END IF;
END;
/
----------------------------------------------------------------
-- Removing posting controls related to the module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Posting Controls Related to Module');
      Posting_Ctrl_Public_API.Remove_Postingctrls_Per_Module('FNDBAS');
   END IF;
END;
/

----------------------------------------------------------------
-- Removing combined control types related to the module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Combined Control Types related to the Module...');
      Posting_Ctrl_Public_API.Rmv_Combctrl_Typs_Per_Module('FNDBAS');
   END IF;
END;
/

----------------------------------------------------------------
-- Removing allowed control types related to the module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Allowed Control Types related to the Module...');
      Posting_Ctrl_Public_API.Remove_Allowed_Comb_Per_Module('FNDBAS');
   END IF;
END;
/

----------------------------------------------------------------
-- Removing control types related to the module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Control Types related to the Module...');
      Posting_Ctrl_Public_API.Remove_Ctrl_Types_Per_Module('FNDBAS');
   END IF;
END;
/

----------------------------------------------------------------
-- Removing posting types related to the module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Control Types related to the Module...');
      Posting_Ctrl_Public_API.Remove_Posting_Typs_Per_Module('FNDBAS');
   END IF;
END;
/
--=======================================================
-- Removing Posting Controls and Related Data End
--=======================================================

--=======================================================
-- Removing Company Templates and Related Data Start
--=======================================================

BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Company Templates and Related Data...');
   END IF;
END;
/

----------------------------------------------------------------
-- Removing company template data related to the module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Company Templates...');
      Create_Company_API.Remove_Company_Templs_Per_Comp('FNDBAS');
   END IF;
END;
/

----------------------------------------------------------------
      -- Removing create company metadata
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Company Template Meta Data...');
      Crecomp_Component_API.Remove_Crecomp_Component('FNDBAS');
   END IF;
END;
/

----------------------------------------------------------------
-- Removing client mappings related to the module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Client Mappings related to the Module...');
      Client_Mapping_API.Remove_Mapping_Per_Module('FNDBAS');
   END IF;
END;
/

--=======================================================
-- Removing Company Templates and Related Data End
--=======================================================


----------------------------------------------------------------
-- Removing dimension metadata owned by module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Dimension Metadata owned by the Module...');
      Xlr_Meta_Util_API.Remove_Dimensions('FNDBAS');
   END IF;
END;
/

----------------------------------------------------------------
      -- Removing fact metadata owned by module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Fact Metadata owned by the Module...');
      Xlr_Meta_Util_API.Remove_Facts('FNDBAS');
   END IF;
END;
/

----------------------------------------------------------------
-- Removing PO entries in repository
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing PO Entries in the Repository...');
      PRES_OBJECT_UTIL_API.Reset_Repository('FNDBAS');
   END IF;
END;
/

----------------------------------------------------------------
-- Removing language related entries
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Language related Entries...');
      LANGUAGE_MODULE_API.Remove__('FNDBAS');
   END IF;
END;
/

----------------------------------------------------------------
-- Removing batch schedules related to the module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Batch Schedules related to the Module...');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('APPLICATION_SEARCH_SYS.ENABLE_SEARCH_DOMAIN__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('APPLICATION_SEARCH_SYS.OPTIMIZE_INDEX__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('APPLICATION_SEARCH_SYS.REBUILD_INDEX__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('APPLICATION_SEARCH_SYS.SYNC_INDEX__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('ARCHIVE_API.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('ARCHIVE_API.CREATE_AND_PRINT_REPORT__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('BATCH_SCHEDULE_API.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('BATCH_SCHEDULE_CHAIN_API.RUN_BATCH_SCHEDULE_CHAIN__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('BATCH_SYS.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('BATCH_SYS.FND_HEAVY_CLEANUP_');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('BATCH_SYS.FND_LIGHT_CLEANUP_');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('BI_TIME_DIMENSION_API.GENERATE_TIME_DATA_SCHEDULE');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('CONNECTIVITY_SYS.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('DATABASE_SYS.ENABLE_ROWKEY');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('DATABASE_SYS.EXECUTE_ANALYZE_SCHEMA__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('DATABASE_SYS.REBUILD_INDEXES__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('DATABASE_SYS.SHRINK_LOB_SEGMENTS');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('DATABASE_SYS.VALIDATE_INDEXES__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('FND_EVENT_MY_MESSAGES_API.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('FND_MONITOR_ENTRY_API.PERFORM_MONITORING__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('FND_SESSION_API.CLEANUP_');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('FUNC_AREA_CONFLICT_CACHE_API.REFRESH_CACHE');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('HISTORY_LOG_UTIL_API.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('IAL_OBJECT_UTIL_API.INITIATE_REPLICATION__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('INSTALL_TEM_SYS.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('OBJECT_CONNECTION_SYS.REMOVE_DANGLING_CONNECTIONS_');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('PRINT_JOB_API.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('SEC_CHECKPOINT_LOG_API.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('SECURITY_SYS.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('SECURITY_SYS.VALIDATE_TIMESPAN_FOR_USERS__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('SERVER_LOG_UTILITY_API.ALERT_LOG_ERRORS_');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('SERVER_LOG_UTILITY_API.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('SERVER_LOG_UTILITY_API.TRANSFER_AUDIT_RECORDS_');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('TRANSACTION_SYS.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('XLR_BA_CLIENT_ARCHIVE_API.CLEANUP__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('XLR_META_UTIL_API.REFRESH_DIMENSIONS');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('XLR_META_UTIL_API.REFRESH_FACTS');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('XLR_META_UTIL_API.REFRESH_MV_INFO__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('XLR_META_UTIL_API.REFRESH_MVIEWS');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('XLR_MV_REFRESH_CATEGORY_API.REFRESH_MV_CATEGORIES');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('XLR_MV_UTIL_API.CLEANUP_MV_LOGS');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('XLR_STRUCTURE_UTIL_API.CLEANUP_STRUCTURE_DATA__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('XLR_TEMPLATE_UTIL_API.CLEANUP_SYSTEM_TEMPLATES__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('XLR_TEMPLATE_UTIL_API.CLEANUP_TRACE_DATA__');
      Batch_SYS.Rem_Cascade_Batch_Schedule_Met('XLR_WRITE_BACK_UTIL_API.CLEANUP_WRITE_BACK_DATA__');

   END IF;
END;
/

----------------------------------------------------------------
-- Removing reports for this module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Reports for the module...');
      Report_SYS.Remove_Report_Definition('FND_SECURITY_PER_OBJECT_REP');
      Report_SYS.Remove_Report_Definition('FND_SECURITY_PER_USER_REP');
      Report_SYS.Remove_Report_Definition('FND_SESSION_REP');
      Report_SYS.Remove_Report_Definition('FUNCTIONAL_AREA_CONFLICT_REP');
      Report_SYS.Remove_Report_Definition('HISTORY_LOG_REP');
      Report_SYS.Remove_Report_Definition('MODULE_REP');
   END IF;
END;
/

----------------------------------------------------------------
-- Removing events related to this module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Events Related to this Module...');
      Event_SYS.Disable_Event('BACKGROUND_JOB_IN_PROGRESS', 'Transaction');
      Event_SYS.Disable_Event('BACKGROUND_JOB_IS_PROCESSED', 'Transaction');
      Event_SYS.Disable_Event('CU_WARNING', 'General');
      Event_SYS.Disable_Event('DATA_ARCHIVE_EXECUTED', 'DataArchiveUtil');
      Event_SYS.Disable_Event('HISTORY_LOG_MODIFIED', 'HistoryLog');
      Event_SYS.Disable_Event('MONITOR_ENTRY_WARNING', 'FndMonitorEntry');
      Event_SYS.Disable_Event('PDF_REPORT_CREATED', 'PrintJob');
      Event_SYS.Disable_Event('REPLICATION_ERROR', 'ReplicationLog');
      Event_SYS.Disable_Event('SECURITY_CHECKPOINT_SUCCESS', 'Security');

   END IF;
END;
/

----------------------------------------------------------------
-- Removing search domain related to the module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Search Domains for the Module...');
      Application_Search_SYS.Remove_Search_Domain('Terms');
      Application_Search_SYS.Remove_Search_Domain('UserProfile');
      Application_Search_SYS.Remove_Search_Domain('Users');

   END IF;
END;
/

----------------------------------------------------------------
-- Removing objects connections related to the module
----------------------------------------------------------------
BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Removing Objects Connections related to the Module...');
      NULL;
   END IF;
END;
/

BEGIN
   IF (SUBSTR(UPPER('&remove_data'),1,1)  = 'Y') THEN
      dbms_output.put_line('Finished Removing Module Specific Data...');
   END IF;
END;
/

--=======================================================
-- DATA REMOVAL SECTION END
--=======================================================


----------------------------------------------------------------
-- Number of synonyms           : 0
-- Number of packages           : 836
-- Number of views              : 551
-- Number of triggers           : 0
-- Number of sequences          : 42
-- Number of materialized views : 0
-- Number of tables             : 403
----------------------------------------------------------------


----------------------------------------------------------------
-- Drop synonyms
----------------------------------------------------------------


----------------------------------------------------------------
-- Drop packages
----------------------------------------------------------------

BEGIN
   dbms_output.put_line('Removing Packages for the Module...');
   Installation_SYS.Remove_Package('ACTIVITY_ENTITY_FILTER_API', TRUE);
   Installation_SYS.Remove_Package('ACTIVITY_ENTITY_USAGE_API', TRUE);
   Installation_SYS.Remove_Package('ACTIVITY_FNDDR_API', TRUE);
   Installation_SYS.Remove_Package('ACTIVITY_GRANT_API', TRUE);
   Installation_SYS.Remove_Package('ACTIVITY_GRANT_FILTER_API', TRUE);
   Installation_SYS.Remove_Package('ACTIVITY_GRANT_FILTER_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('ACTIVITY_PACKAGE_API', TRUE);
   Installation_SYS.Remove_Package('ACTIVITY_PACKAGE_DIAGRAM_API', TRUE);
   Installation_SYS.Remove_Package('ACTIVITY_TYPE_FNDDR_API', TRUE);
   Installation_SYS.Remove_Package('ADDRESS_LABEL_API', TRUE);
   Installation_SYS.Remove_Package('AGENDA_API', TRUE);
   Installation_SYS.Remove_Package('API_SERVICE_LEVEL_API', TRUE);
   Installation_SYS.Remove_Package('APPLICATION_MESSAGE_API', TRUE);
   Installation_SYS.Remove_Package('APPLICATION_SEARCH_ADMIN_SYS', TRUE);
   Installation_SYS.Remove_Package('APPLICATION_SEARCH_INDEX_SYS', TRUE);
   Installation_SYS.Remove_Package('APPLICATION_SEARCH_RUNTIME_SYS', TRUE);
--   Installation_SYS.Remove_Package('APPLICATION_SEARCH_SYS', TRUE);
   Installation_SYS.Remove_Package('ARCHIVE_API', TRUE);
   Installation_SYS.Remove_Package('ARCHIVE_DISTRIBUTION_API', TRUE);
   Installation_SYS.Remove_Package('ARCHIVE_FILE_NAME_API', TRUE);
   Installation_SYS.Remove_Package('ARCHIVE_PARAMETER_API', TRUE);
   Installation_SYS.Remove_Package('ARCHIVE_VARIABLE_API', TRUE);
   Installation_SYS.Remove_Package('ARGUMENT_STORAGE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('ARGUMENT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('ASSERT_SYS', TRUE);
   Installation_SYS.Remove_Package('ASSOCIATION_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('ATTRIBUTE_STATE_VALIDATION_API', TRUE);
   Installation_SYS.Remove_Package('ATTRIBUTE_VALIDATION_RULE_API', TRUE);
   Installation_SYS.Remove_Package('AUTHORIZATION_OBJECT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('BASIC_DATA_TRANSLATION_API', TRUE);
   Installation_SYS.Remove_Package('BATCH_JOB_API', TRUE);
   Installation_SYS.Remove_Package('BATCH_QUEUE_API', TRUE);
   Installation_SYS.Remove_Package('BATCH_QUEUE_METHOD_API', TRUE);
   Installation_SYS.Remove_Package('BATCH_SCHEDULE_API', TRUE);
   Installation_SYS.Remove_Package('BATCH_SCHEDULE_CHAIN_API', TRUE);
   Installation_SYS.Remove_Package('BATCH_SCHEDULE_CHAIN_PAR_API', TRUE);
   Installation_SYS.Remove_Package('BATCH_SCHEDULE_CHAIN_STEP_API', TRUE);
   Installation_SYS.Remove_Package('BATCH_SCHEDULE_METHOD_API', TRUE);
   Installation_SYS.Remove_Package('BATCH_SCHEDULE_METHOD_PAR_API', TRUE);
   Installation_SYS.Remove_Package('BATCH_SCHEDULE_PAR_API', TRUE);
   Installation_SYS.Remove_Package('BATCH_SCHEDULE_TYPE_API', TRUE);
--   Installation_SYS.Remove_Package('BATCH_SYS', TRUE);
   Installation_SYS.Remove_Package('BI_TIME_DIMENSION_API', TRUE);
   Installation_SYS.Remove_Package('BI_UTILITY_API', TRUE);
   Installation_SYS.Remove_Package('BINARY_OBJECT_API', TRUE);
   Installation_SYS.Remove_Package('BINARY_OBJECT_DATA_BLOCK_API', TRUE);
   Installation_SYS.Remove_Package('BINARY_OBJECT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('BIZ_API_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('BODY_TYPES_API', TRUE);
   Installation_SYS.Remove_Package('BULLETIN_BOARD_MESSAGES_API', TRUE);
   Installation_SYS.Remove_Package('BULLETIN_BOARD_TOPIC_USERS_API', TRUE);
   Installation_SYS.Remove_Package('BULLETIN_BOARD_TOPICS_API', TRUE);
   Installation_SYS.Remove_Package('BULLETIN_BOARD_USER_LEVEL_API', TRUE);
   Installation_SYS.Remove_Package('CACHE_MANAGEMENT_API', TRUE);
   Installation_SYS.Remove_Package('CACHED_ARTIFACT_API', TRUE);
   Installation_SYS.Remove_Package('CACHED_ARTIFACT_ELEMENT_API', TRUE);
   Installation_SYS.Remove_Package('CARDINALITY_API', TRUE);
   Installation_SYS.Remove_Package('CLIENT_DIAGRAM_API', TRUE);
   Installation_SYS.Remove_Package('CLIENT_PACKAGE_API', TRUE);
   Installation_SYS.Remove_Package('CLIENT_PACKAGE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('CLIENT_PLUG_IN_ACTIVITY_API', TRUE);
   Installation_SYS.Remove_Package('CLIENT_PLUG_IN_API', TRUE);
   Installation_SYS.Remove_Package('CLIENT_PLUG_IN_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('CLIENT_PROFILE_API', TRUE);
   Installation_SYS.Remove_Package('CLIENT_PROFILE_VALUE_API', TRUE);
   Installation_SYS.Remove_Package('CLIENT_SYS', TRUE);
   Installation_SYS.Remove_Package('COLUMN_FLAGS_API', TRUE);
   Installation_SYS.Remove_Package('COMMAND_SYS', TRUE);
   Installation_SYS.Remove_Package('COMMON_MESSAGES_API', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_DEPENDENCY_API', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_DEPENDENCY_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FILE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PATCH_API', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PATCH_ROW_API', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('CONDITION_PART_API', TRUE);
   Installation_SYS.Remove_Package('CONFIG_PARAMETER_API', TRUE);
   Installation_SYS.Remove_Package('CONFIG_PARAMETER_AREA_API', TRUE);
   Installation_SYS.Remove_Package('CONFIG_PARAMETER_GROUP_API', TRUE);
   Installation_SYS.Remove_Package('CONFIG_PARAMETER_INSTANCE_API', TRUE);
   Installation_SYS.Remove_Package('CONNECT_DIRECTION_API', TRUE);
   Installation_SYS.Remove_Package('CONNECT_DOC_FORMAT_API', TRUE);
   Installation_SYS.Remove_Package('CONNECTIVITY_SYS', TRUE);
   Installation_SYS.Remove_Package('CONTEXT_API', TRUE);
   Installation_SYS.Remove_Package('CONTEXT_SUBSTITUTION_VAR_API', TRUE);
   Installation_SYS.Remove_Package('CRYSTAL_EXPORT_TYPES_API', TRUE);
   Installation_SYS.Remove_Package('CRYSTAL_WEB_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('CURRENT_ORACLE_USER_SYS', TRUE);
   Installation_SYS.Remove_Package('CUSTOM_MENU_API', TRUE);
   Installation_SYS.Remove_Package('CUSTOM_MENU_EXP_PAR_API', TRUE);
   Installation_SYS.Remove_Package('CUSTOM_MENU_KEY_TRANS_API', TRUE);
   Installation_SYS.Remove_Package('CUSTOM_MENU_TEXT_API', TRUE);
   Installation_SYS.Remove_Package('CUSTOM_MENU_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('CUSTOM_MENU_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('CUSTOM_OBJECTS_SYS', TRUE);
   Installation_SYS.Remove_Package('DATA_ARCHIVE_DESTINATION_API', TRUE);
   Installation_SYS.Remove_Package('DATA_ARCHIVE_EXEC_ATTR_API', TRUE);
   Installation_SYS.Remove_Package('DATA_ARCHIVE_LOG_API', TRUE);
   Installation_SYS.Remove_Package('DATA_ARCHIVE_OBJECT_API', TRUE);
   Installation_SYS.Remove_Package('DATA_ARCHIVE_ORDER_API', TRUE);
   Installation_SYS.Remove_Package('DATA_ARCHIVE_ORDER_EXEC_API', TRUE);
   Installation_SYS.Remove_Package('DATA_ARCHIVE_SOURCE_API', TRUE);
   Installation_SYS.Remove_Package('DATA_ARCHIVE_SOURCE_ATTR_API', TRUE);
   Installation_SYS.Remove_Package('DATA_ARCHIVE_SYS', TRUE);
   Installation_SYS.Remove_Package('DATA_ARCHIVE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('DATA_ARCHIVE_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('DATABASE_OBJECT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('DATATYPE_API', TRUE);
   Installation_SYS.Remove_Package('DB_SCRIPT_REGISTER_API', TRUE);
   Installation_SYS.Remove_Package('DB_SCRIPT_REGISTER_DETAIL_API', TRUE);
   Installation_SYS.Remove_Package('DEBUG_LEVELS_API', TRUE);
   Installation_SYS.Remove_Package('DEFERRED_JOB_API', TRUE);
   Installation_SYS.Remove_Package('DEFERRED_JOB_STATE_API', TRUE);
   Installation_SYS.Remove_Package('DEFERRED_JOB_STATUS_API', TRUE);
   Installation_SYS.Remove_Package('DELETE_BEHAVIOR_API', TRUE);
   Installation_SYS.Remove_Package('DESIGN_SYS', TRUE);
   Installation_SYS.Remove_Package('DETAIL_VALIDATION_API', TRUE);
   Installation_SYS.Remove_Package('DICTIONARY_SYS', TRUE);
   Installation_SYS.Remove_Package('DISTRIBUTION_GROUP_API', TRUE);
   Installation_SYS.Remove_Package('DISTRIBUTION_GROUP_MEMBER_API', TRUE);
   Installation_SYS.Remove_Package('DISTRIBUTION_GROUP_PROT_API', TRUE);
   Installation_SYS.Remove_Package('DOCUMENT_LIFECYCLE_API', TRUE);
   Installation_SYS.Remove_Package('DOMAIN_SYS', TRUE);
   Installation_SYS.Remove_Package('DRAFT_API', TRUE);
   Installation_SYS.Remove_Package('DSOD_API', TRUE);
   Installation_SYS.Remove_Package('DSOD_CRITERIA_API', TRUE);
   Installation_SYS.Remove_Package('DSOD_DISPLAY_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('DSOD_GROUPING_API', TRUE);
   Installation_SYS.Remove_Package('DSOD_SET_API', TRUE);
   Installation_SYS.Remove_Package('DSOD_SORTING_API', TRUE);
   Installation_SYS.Remove_Package('DSOD_STRUCTURE_REQUEST_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_ASS_STATE_VALID_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_ASSOCIATION_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_ASSOCIATION_ATTR_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_ATTRIBUTE_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_CATEGORY_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_DIAGRAM_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_PACKAGE_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_PACKAGE_DIAGRAM_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_STATE_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_STATE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_STATES_API', TRUE);
   Installation_SYS.Remove_Package('ENTITY_USAGE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('ENUMERATION_VALUE_API', TRUE);
   Installation_SYS.Remove_Package('ERROR_SYS', TRUE);
--   Installation_SYS.Remove_Package('EVENT_SYS', TRUE);
   Installation_SYS.Remove_Package('EXCEL_REPORT_ARCHIVE_API', TRUE);
   Installation_SYS.Remove_Package('EXECUTE_AS_API', TRUE);
   Installation_SYS.Remove_Package('FEATURE_ACTIVITY_API', TRUE);
   Installation_SYS.Remove_Package('FEATURE_API', TRUE);
   Installation_SYS.Remove_Package('FEATURE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('FEATURE_WIDGET_API', TRUE);
   Installation_SYS.Remove_Package('FILE_CATEGORY_API', TRUE);
   Installation_SYS.Remove_Package('FILE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('FILTER_API', TRUE);
   Installation_SYS.Remove_Package('FILTER_PARAMETER_API', TRUE);
   Installation_SYS.Remove_Package('FILTER_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('FIRST_WEEK_OF_YEAR_API', TRUE);
   Installation_SYS.Remove_Package('FND_BOOLEAN_API', TRUE);
   Installation_SYS.Remove_Package('FND_CODE_TEMPLATE_API', TRUE);
   Installation_SYS.Remove_Package('FND_DATA_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('FND_EVENT_ACTION_API', TRUE);
   Installation_SYS.Remove_Package('FND_EVENT_ACTION_SUBSCRIBE_API', TRUE);
   Installation_SYS.Remove_Package('FND_EVENT_ACTION_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('FND_EVENT_API', TRUE);
   Installation_SYS.Remove_Package('FND_EVENT_MY_MESSAGES_API', TRUE);
   Installation_SYS.Remove_Package('FND_EVENT_PARAMETER_API', TRUE);
   Installation_SYS.Remove_Package('FND_EVENT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('FND_GRANT_ROLE_API', TRUE);
   Installation_SYS.Remove_Package('FND_LICENSE_API', TRUE);
   Installation_SYS.Remove_Package('FND_MONITOR_CATEGORY_API', TRUE);
   Installation_SYS.Remove_Package('FND_MONITOR_ENTRY_API', TRUE);
   Installation_SYS.Remove_Package('FND_NODE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('FND_NOTE_BOOK_API', TRUE);
   Installation_SYS.Remove_Package('FND_NOTE_PAGE_API', TRUE);
   Installation_SYS.Remove_Package('FND_ROLE_API', TRUE);
   Installation_SYS.Remove_Package('FND_ROLE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('FND_SECURITY_PER_OBJECT_RPI', TRUE);
   Installation_SYS.Remove_Package('FND_SECURITY_PER_USER_RPI', TRUE);
   Installation_SYS.Remove_Package('FND_SESSION_API', TRUE);
   Installation_SYS.Remove_Package('FND_SESSION_RPI', TRUE);
   Installation_SYS.Remove_Package('FND_SESSION_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('FND_SETTING_API', TRUE);
   Installation_SYS.Remove_Package('FND_TRANSLATION_API', TRUE);
   Installation_SYS.Remove_Package('FND_USER_API', TRUE);
   Installation_SYS.Remove_Package('FND_USER_PROPERTY_API', TRUE);
   Installation_SYS.Remove_Package('FND_USER_PROPERTY_SEC_API', TRUE);
   Installation_SYS.Remove_Package('FND_USER_ROLE_API', TRUE);
   Installation_SYS.Remove_Package('FND_WEEK_DAY_API', TRUE);
   Installation_SYS.Remove_Package('FND_WORKSPACE_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('FND_YES_NO_API', TRUE);
   Installation_SYS.Remove_Package('FNDCN_MESSAGE_ARCHIVE_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('FNDRR_CLIENT_PROFILE_API', TRUE);
   Installation_SYS.Remove_Package('FNDRR_CLIENT_PROFILE_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('FNDRR_CLIENT_PROFILE_VALUE_API', TRUE);
   Installation_SYS.Remove_Package('FNDRR_USER_CLIENT_PROFILE_API', TRUE);
   Installation_SYS.Remove_Package('FUNC_AREA_CONFLICT_CACHE_API', TRUE);
   Installation_SYS.Remove_Package('FUNC_AREA_CONFLICT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('FUNC_AREA_SEC_CACHE_API', TRUE);
   Installation_SYS.Remove_Package('FUNC_AREA_SEC_OBJECT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('FUNCTIONAL_AREA_ACTIVITY_API', TRUE);
   Installation_SYS.Remove_Package('FUNCTIONAL_AREA_CONFLICT_API', TRUE);
   Installation_SYS.Remove_Package('FUNCTIONAL_AREA_CONFLICT_RPI', TRUE);
   Installation_SYS.Remove_Package('FUNCTIONAL_AREA_METHOD_API', TRUE);
   Installation_SYS.Remove_Package('FUNCTIONAL_AREA_VIEW_API', TRUE);
   Installation_SYS.Remove_Package('GENERAL_SYS', TRUE);
   Installation_SYS.Remove_Package('HANDLER_API', TRUE);
   Installation_SYS.Remove_Package('HANDLER_METHOD_API', TRUE);
   Installation_SYS.Remove_Package('HANDLER_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('HINT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('HISTORY_LOG_API', TRUE);
   Installation_SYS.Remove_Package('HISTORY_LOG_ATTRIBUTE_API', TRUE);
   Installation_SYS.Remove_Package('HISTORY_LOG_RPI', TRUE);
   Installation_SYS.Remove_Package('HISTORY_LOG_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('HISTORY_SETTING_API', TRUE);
   Installation_SYS.Remove_Package('HISTORY_SETTING_ATTRIBUTE_API', TRUE);
   Installation_SYS.Remove_Package('HISTORY_SETTING_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('HISTORY_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('I_FACE_CONTROL_PARAMETER_API', TRUE);
   Installation_SYS.Remove_Package('IAL_OBJECT_API', TRUE);
   Installation_SYS.Remove_Package('IAL_OBJECT_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('IMPLEMENTATION_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('IN_MESSAGE_API', TRUE);
   Installation_SYS.Remove_Package('IN_MESSAGE_LINE_API', TRUE);
   Installation_SYS.Remove_Package('IN_MESSAGE_LOADED_API', TRUE);
   Installation_SYS.Remove_Package('IN_MESSAGE_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('INDEX_COLUMN_API', TRUE);
   Installation_SYS.Remove_Package('INFORMATION_OBJECT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('INSTALL_TEM_SYS', TRUE);
   Installation_SYS.Remove_Package('INSTALLATION_SITE_API', TRUE);
--   Installation_SYS.Remove_Package('INSTALLATION_SYS', TRUE);
   Installation_SYS.Remove_Package('INSTALLED_COMPONENT_SYS', TRUE);
   Installation_SYS.Remove_Package('INTREP_TYPES_API', TRUE);
   Installation_SYS.Remove_Package('ITEM_OWNER_API', TRUE);
   Installation_SYS.Remove_Package('J2EE_APPLICATION_API', TRUE);
   Installation_SYS.Remove_Package('J2EE_MODULE_API', TRUE);
   Installation_SYS.Remove_Package('JOB_TYPES_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_ATTRIBUTE_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_ATTRIBUTE_NAME_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_CODE_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_CODE_STATUS_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_CONNECTION_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_CONTENT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_CONTEXT_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_CONTEXT_MAIN_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_CONTEXT_SUB_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_DESTINATION_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_DRIVER_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_EXPORT_METHOD_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_FILE_EXPORT_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_FILE_IMPORT_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_FONT_MAPPING_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_IMPORT_METHOD_API', TRUE);
--   Installation_SYS.Remove_Package('LANGUAGE_MODULE_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_OBSOLETE_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_PROPERTY_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_SOURCE_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_SOURCE_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_SYS', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_SYS_IMP_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_TR_METHOD_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_TR_STATUS_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_TRANSLATION_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_TRANSLATION_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('LANGUAGE_WRITE_PROTECT_API', TRUE);
   Installation_SYS.Remove_Package('LAYOUT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('LDAP_CONFIGURATION_API', TRUE);
   Installation_SYS.Remove_Package('LDAP_DOMAIN_CONFIG_API', TRUE);
   Installation_SYS.Remove_Package('LDAP_MAPPING_API', TRUE);
   Installation_SYS.Remove_Package('LICENSE_CATEGORY_API', TRUE);
   Installation_SYS.Remove_Package('LOGICAL_PRINTER_API', TRUE);
   Installation_SYS.Remove_Package('LOGICAL_UNIT_API', TRUE);
   Installation_SYS.Remove_Package('LOGICAL_UNIT_DETAILS_API', TRUE);
   Installation_SYS.Remove_Package('LOGICAL_UNIT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('LOGIN_SYS', TRUE);
   Installation_SYS.Remove_Package('LU_ATTRIBUTE_API', TRUE);
   Installation_SYS.Remove_Package('LU_CATEGORY_API', TRUE);
   Installation_SYS.Remove_Package('LU_OPERATION_API', TRUE);
   Installation_SYS.Remove_Package('LU_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('MAPPED_ENTITY_API', TRUE);
   Installation_SYS.Remove_Package('MESSAGE_ARCHIVE_ADDRESS_API', TRUE);
   Installation_SYS.Remove_Package('MESSAGE_ARCHIVE_API', TRUE);
   Installation_SYS.Remove_Package('MESSAGE_ARCHIVE_BODY_API', TRUE);
   Installation_SYS.Remove_Package('MESSAGE_ARCHIVE_SEARCH_API', TRUE);
   Installation_SYS.Remove_Package('MESSAGE_BODY_API', TRUE);
   Installation_SYS.Remove_Package('MESSAGE_CLASS_API', TRUE);
   Installation_SYS.Remove_Package('MESSAGE_MEDIA_API', TRUE);
   Installation_SYS.Remove_Package('MESSAGE_QUEUE_API', TRUE);
   Installation_SYS.Remove_Package('MESSAGE_RECEIVER_API', TRUE);
   Installation_SYS.Remove_Package('MESSAGE_STATE_TYPES_API', TRUE);
   Installation_SYS.Remove_Package('MESSAGE_SYS', TRUE);
   Installation_SYS.Remove_Package('METHOD_FILTER_API', TRUE);
   Installation_SYS.Remove_Package('METHOD_IMPLEMENTATION_API', TRUE);
   Installation_SYS.Remove_Package('METHOD_PARAMETER_API', TRUE);
   Installation_SYS.Remove_Package('MOBILE_CLIENT_PACKAGE_API', TRUE);
   Installation_SYS.Remove_Package('MOBILE_ENTITY_SYNCH_API', TRUE);
   Installation_SYS.Remove_Package('MOBILE_OPTIMIZER_DATA_API', TRUE);
   Installation_SYS.Remove_Package('MOBILE_USER_CACHE_API', TRUE);
   Installation_SYS.Remove_Package('MODEL_IMPORT_LOG_API', TRUE);
   Installation_SYS.Remove_Package('MODEL_WORKSPACE_API', TRUE);
--   Installation_SYS.Remove_Package('MODULE_API', TRUE);
   Installation_SYS.Remove_Package('MODULE_DB_PATCH_API', TRUE);
   Installation_SYS.Remove_Package('MODULE_DEPENDENCY_API', TRUE);
   Installation_SYS.Remove_Package('MODULE_DIAGRAM_API', TRUE);
   Installation_SYS.Remove_Package('MODULE_FNDDR_API', TRUE);
   Installation_SYS.Remove_Package('MODULE_RPI', TRUE);
   Installation_SYS.Remove_Package('MODULE_SYSTEM_DOC_API', TRUE);
   Installation_SYS.Remove_Package('MODULE_TRANSLATE_ATTR_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('MOUNTPOINT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('MY_TODO_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('NLS_CALENDAR_API', TRUE);
   Installation_SYS.Remove_Package('NOTE_API', TRUE);
   Installation_SYS.Remove_Package('NOTE_BOOK_API', TRUE);
   Installation_SYS.Remove_Package('NUM_FMT_CURR_NEG_PATTERN_API', TRUE);
   Installation_SYS.Remove_Package('NUM_FMT_CURR_POS_PATTERN_API', TRUE);
   Installation_SYS.Remove_Package('NUM_FMT_NUM_NEG_PATTERN_API', TRUE);
   Installation_SYS.Remove_Package('NUM_FMT_PERC_NEG_PATTERN_API', TRUE);
   Installation_SYS.Remove_Package('NUM_FMT_PERC_POS_PATTERN_API', TRUE);
   Installation_SYS.Remove_Package('OBJECT_CONNECTION_SYS', TRUE);
   Installation_SYS.Remove_Package('ORACLE_ACCOUNT_API', TRUE);
   Installation_SYS.Remove_Package('ORACLE_DATATYPE_API', TRUE);
   Installation_SYS.Remove_Package('ORACLE_PROFILE_API', TRUE);
   Installation_SYS.Remove_Package('ORACLE_PROFILE_LIMITS_API', TRUE);
   Installation_SYS.Remove_Package('OUT_MESSAGE_API', TRUE);
   Installation_SYS.Remove_Package('OUT_MESSAGE_LINE_API', TRUE);
   Installation_SYS.Remove_Package('OUT_MESSAGE_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('PACKAGE_DEPENDENCY_API', TRUE);
   Installation_SYS.Remove_Package('PACKAGE_DIAGRAM_API', TRUE);
   Installation_SYS.Remove_Package('PAPER_FORMAT_API', TRUE);
   Installation_SYS.Remove_Package('PARAMETER_API', TRUE);
   Installation_SYS.Remove_Package('PARAMETER_DIRECTION_API', TRUE);
   Installation_SYS.Remove_Package('PARAMETER_GROUP_TYPES_API', TRUE);
   Installation_SYS.Remove_Package('PARAMETER_TYPES_API', TRUE);
   Installation_SYS.Remove_Package('PATTERN_TYPES_API', TRUE);
   Installation_SYS.Remove_Package('PDF_ARCHIVE_API', TRUE);
   Installation_SYS.Remove_Package('PERFORMANCE_ANALYZE_API', TRUE);
   Installation_SYS.Remove_Package('PERMISSION_SET_FILTER_API', TRUE);
   Installation_SYS.Remove_Package('PERMISSION_SET_FILTER_PAR_API', TRUE);
   Installation_SYS.Remove_Package('PIRINT_AGENT_DEBUG_LEVEL_API', TRUE);
   Installation_SYS.Remove_Package('PLSQL_METHOD_API', TRUE);
   Installation_SYS.Remove_Package('PLSQL_PACKAGE_API', TRUE);
   Installation_SYS.Remove_Package('PLSQL_PARAMETER_API', TRUE);
   Installation_SYS.Remove_Package('PLSQLAP_BUFFER_API', TRUE);
   Installation_SYS.Remove_Package('PLSQLAP_RECORD_API', TRUE);
   Installation_SYS.Remove_Package('PLSQLAP_SERVER_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_CHANGE_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_CHANGE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_DEP_CHANGE_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_DEP_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_DEPENDENCY_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_DESCRIPTION_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_EXCLUDE_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_GRANT_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_INCLUDE_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_INCLUDE_SEC_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_SEC_CHANGE_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_SEC_EXPORT_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_SEC_SUB_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_SECURITY_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_SECURITY_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('PRES_OBJECT_TYPE_API', TRUE);
--   Installation_SYS.Remove_Package('PRES_OBJECT_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('PRINT_JOB_API', TRUE);
   Installation_SYS.Remove_Package('PRINT_JOB_CONTENTS_API', TRUE);
   Installation_SYS.Remove_Package('PRINT_JOB_OWNER_API', TRUE);
   Installation_SYS.Remove_Package('PRINT_JOB_STATUS_API', TRUE);
   Installation_SYS.Remove_Package('PRINT_QUEUE_API', TRUE);
   Installation_SYS.Remove_Package('PROJECT_SETTING_API', TRUE);
   Installation_SYS.Remove_Package('QUERY_HINT_COL_API', TRUE);
   Installation_SYS.Remove_Package('QUERY_HINT_INDEX_API', TRUE);
   Installation_SYS.Remove_Package('QUERY_HINT_TABLE_API', TRUE);
   Installation_SYS.Remove_Package('QUERY_HINT_UTILITY_API', TRUE);
   Installation_SYS.Remove_Package('QUERY_HINT_VIEW_API', TRUE);
   Installation_SYS.Remove_Package('QUEUE_TYPES_API', TRUE);
   Installation_SYS.Remove_Package('QUICK_REPORT_API', TRUE);
   Installation_SYS.Remove_Package('QUICK_REPORT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('RECEIVE_ATTR_API', TRUE);
   Installation_SYS.Remove_Package('RECEIVE_ATTR_GROUP_API', TRUE);
   Installation_SYS.Remove_Package('RECEIVE_CONDITION_API', TRUE);
   Installation_SYS.Remove_Package('RECEIVE_GROUP_API', TRUE);
   Installation_SYS.Remove_Package('RECEIVE_OBJECT_API', TRUE);
   Installation_SYS.Remove_Package('RECURRENCE_AGENDA_API', TRUE);
   Installation_SYS.Remove_Package('RECURRENCE_PATTERN_API', TRUE);
   Installation_SYS.Remove_Package('REFERENCE_SYS', TRUE);
   Installation_SYS.Remove_Package('REMOTE_DEBUG_SYS', TRUE);
   Installation_SYS.Remove_Package('REMOTE_PRINTER_MAPPING_API', TRUE);
   Installation_SYS.Remove_Package('REMOTE_PRINTING_NODE_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_ATTR_DEF_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_ATTR_GROUP_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_ATTR_GROUP_DEF_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_CONDITION_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_DESIGN_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_GROUP_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_LOG_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_OBJECT_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_OBJECT_DEF_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_QUEUE_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_RECEIVER_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_SENDER_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_STATISTICS_API', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_SYS', TRUE);
   Installation_SYS.Remove_Package('REPLICATION_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_ARCHIVING_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_CATEGORY_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_COLUMN_DEFINITION_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_DEFINITION_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_FONT_DEFINITION_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_FORMAT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_LAYOUT_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_LAYOUT_DEFINITION_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_LAYOUT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_LAYOUT_TYPE_CONFIG_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_LU_DEFINITION_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_PARAMETER_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_PDF_INSERT_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_PLUGIN_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_RESULT_GEN_CONFIG_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_RULE_ACTION_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_RULE_CONDITION_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_RULE_DEFINITION_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_RULE_LOG_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_SCHEMA_API', TRUE);
--   Installation_SYS.Remove_Package('REPORT_SYS', TRUE);
   Installation_SYS.Remove_Package('REPORT_TEXT_API', TRUE);
   Installation_SYS.Remove_Package('REPORT_USER_SETTINGS_API', TRUE);
   Installation_SYS.Remove_Package('ROUTE_ADDRESS_API', TRUE);
   Installation_SYS.Remove_Package('ROUTE_ADDRESS_REFERENCE_API', TRUE);
   Installation_SYS.Remove_Package('ROUTE_CONDITION_API', TRUE);
   Installation_SYS.Remove_Package('SEARCH_CRITERIA_API', TRUE);
   Installation_SYS.Remove_Package('SEARCH_DOMAIN_DOCUMENT_API', TRUE);
   Installation_SYS.Remove_Package('SEARCH_DOMAIN_GROUP_API', TRUE);
   Installation_SYS.Remove_Package('SEARCH_DOMAIN_GROUP_MEMBER_API', TRUE);
   Installation_SYS.Remove_Package('SEARCH_DOMAIN_RUNTIME_API', TRUE);
   Installation_SYS.Remove_Package('SEARCH_DOMAIN_UNSYNCHED_API', TRUE);
   Installation_SYS.Remove_Package('SEC_CHECKPOINT_GATE_API', TRUE);
   Installation_SYS.Remove_Package('SEC_CHECKPOINT_GATE_PARAM_API', TRUE);
   Installation_SYS.Remove_Package('SEC_CHECKPOINT_LOG_API', TRUE);
   Installation_SYS.Remove_Package('SECURITY_CHECKPOINT_METHOD_API', TRUE);
   Installation_SYS.Remove_Package('SECURITY_SYS', TRUE);
   Installation_SYS.Remove_Package('SELECTION_API', TRUE);
   Installation_SYS.Remove_Package('SELECTION_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('SERV_PACKAGE_DEPENDENCY_API', TRUE);
   Installation_SYS.Remove_Package('SERVER_LOG_API', TRUE);
   Installation_SYS.Remove_Package('SERVER_LOG_CATEGORY_API', TRUE);
   Installation_SYS.Remove_Package('SERVER_LOG_UTILITY_API', TRUE);
   Installation_SYS.Remove_Package('SERVER_PACKAGE_API', TRUE);
   Installation_SYS.Remove_Package('SERVER_PACKAGE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('SITE_TEXT_API', TRUE);
   Installation_SYS.Remove_Package('SOD_CACHE_API', TRUE);
   Installation_SYS.Remove_Package('SOX_FUNCTIONAL_AREA_API', TRUE);
   Installation_SYS.Remove_Package('STATE_DIAGRAM_API', TRUE);
   Installation_SYS.Remove_Package('STATE_EVENT_API', TRUE);
   Installation_SYS.Remove_Package('STEREOTYPE_API', TRUE);
   Installation_SYS.Remove_Package('STORAGE_ATTRIBUTE_API', TRUE);
   Installation_SYS.Remove_Package('STORAGE_DIAGRAM_API', TRUE);
   Installation_SYS.Remove_Package('STORAGE_OBJECT_API', TRUE);
   Installation_SYS.Remove_Package('STORAGE_OBJECT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('STORAGE_OPERATION_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('STORAGE_PACKAGE_API', TRUE);
   Installation_SYS.Remove_Package('SUPPORT_LEVEL_API', TRUE);
   Installation_SYS.Remove_Package('SYSTEM_PRIVILEGE_API', TRUE);
   Installation_SYS.Remove_Package('SYSTEM_PRIVILEGE_GRANT_API', TRUE);
   Installation_SYS.Remove_Package('TABLE_INDEX_API', TRUE);
   Installation_SYS.Remove_Package('TARGET_API', TRUE);
   Installation_SYS.Remove_Package('TEMPLATE_API', TRUE);
   Installation_SYS.Remove_Package('TERM_ALERT_INDICATOR_API', TRUE);
   Installation_SYS.Remove_Package('TERM_API', TRUE);
   Installation_SYS.Remove_Package('TERM_BINDING_MAIN_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('TERM_DEF_HISTORY_EVENT_API', TRUE);
   Installation_SYS.Remove_Package('TERM_DEFINITION_HISTORY_API', TRUE);
   Installation_SYS.Remove_Package('TERM_DEFINITION_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('TERM_DISPLAY_NAME_API', TRUE);
   Installation_SYS.Remove_Package('TERM_DISPLAY_NAME_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('TERM_DOMAIN_API', TRUE);
   Installation_SYS.Remove_Package('TERM_HISTORY_API', TRUE);
   Installation_SYS.Remove_Package('TERM_HISTORY_EVENT_API', TRUE);
   Installation_SYS.Remove_Package('TERM_OWNER_API', TRUE);
   Installation_SYS.Remove_Package('TERM_OWNER_MODULE_API', TRUE);
   Installation_SYS.Remove_Package('TERM_PROPOSAL_QUALITY_API', TRUE);
   Installation_SYS.Remove_Package('TERM_RELATION_API', TRUE);
   Installation_SYS.Remove_Package('TERM_RELATION_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('TERM_STOP_LIST_API', TRUE);
   Installation_SYS.Remove_Package('TERM_STOP_LIST_REASON_API', TRUE);
   Installation_SYS.Remove_Package('TERM_TECHNICAL_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('TERM_TRANSLATED_DEFINITION_API', TRUE);
   Installation_SYS.Remove_Package('TERM_TRANSLATED_NAME_API', TRUE);
   Installation_SYS.Remove_Package('TERM_TRANSLATION_APPROVAL_API', TRUE);
   Installation_SYS.Remove_Package('TERM_USAGE_DEFINITION_API', TRUE);
   Installation_SYS.Remove_Package('TERM_USAGE_IDENTIFIER_API', TRUE);
   Installation_SYS.Remove_Package('TODO_FLAG_API', TRUE);
   Installation_SYS.Remove_Package('TODO_FOLDER_API', TRUE);
   Installation_SYS.Remove_Package('TODO_FOLDER_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('TODO_HISTORY_API', TRUE);
   Installation_SYS.Remove_Package('TODO_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('TODO_ITEM_RECEIVER_API', TRUE);
   Installation_SYS.Remove_Package('TODO_ITEM_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('TODO_PRIORITY_API', TRUE);
   Installation_SYS.Remove_Package('TRACE_SYS', TRUE);
   Installation_SYS.Remove_Package('TRANSACTION_SYS', TRUE);
   Installation_SYS.Remove_Package('TRANSACTIONAL_BEHAVIOR_API', TRUE);
   Installation_SYS.Remove_Package('TRANSFER_OPTION_ENABLED_API', TRUE);
   Installation_SYS.Remove_Package('UML_DIAGRAM_API', TRUE);
   Installation_SYS.Remove_Package('UPDATE_ACTION_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('UPDATE_OBJECT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('USER_CLIENT_PROFILE_API', TRUE);
   Installation_SYS.Remove_Package('USER_GLOBAL_API', TRUE);
   Installation_SYS.Remove_Package('USER_GROUP_API', TRUE);
   Installation_SYS.Remove_Package('USER_GROUP_USER_API', TRUE);
   Installation_SYS.Remove_Package('USER_PROF_BIN_VAL_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('USER_PROFILE_SYS', TRUE);
   Installation_SYS.Remove_Package('UTILITY_SYS', TRUE);
   Installation_SYS.Remove_Package('VALIDATION_OBJECT_API', TRUE);
   Installation_SYS.Remove_Package('VIEW_ASSOCIATION_API', TRUE);
   Installation_SYS.Remove_Package('VIEW_ATTRIBUTE_API', TRUE);
   Installation_SYS.Remove_Package('VIEW_FNDDR_API', TRUE);
   Installation_SYS.Remove_Package('VIEW_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('VISIBILITY_API', TRUE);
   Installation_SYS.Remove_Package('WEB_SERVICES_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('WIDGET_ACTIVITY_API', TRUE);
   Installation_SYS.Remove_Package('WIDGET_API', TRUE);
   Installation_SYS.Remove_Package('WORKSPACE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_ANALYZE_RESULT_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_BA_CLIENT_ARCHIVE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_BA_CLIENT_ARCHIVE_DIST_API', TRUE);
   Installation_SYS.Remove_Package('XLR_CLOB_WRITER_API', TRUE);
   Installation_SYS.Remove_Package('XLR_CRITERIA_FUNCTION_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DEF_FACT_NAV_FOLDER_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DIM_ITEM_STRUCT_INFO_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DIM_REF_RELATIONS_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DIM_SOURCE_HINT_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DIM_SOURCE_IX_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DIM_SOURCE_IX_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DIMENSION_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DIMENSION_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DIMENSION_LOV_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DIMENSION_LOV_SORT_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DIMENSION_PARENT_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DIMENSION_SOURCE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DIMENSION_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DOCUMENT_CATEGORY_API', TRUE);
   Installation_SYS.Remove_Package('XLR_DSOD_SET_CATEGORY_API', TRUE);
   Installation_SYS.Remove_Package('XLR_ENABLED_SOURCE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_DETAIL_DIM_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_DIM_ID_DEF_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_DIM_JOIN_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_DIM_JOIN_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_DIM_SRC_INDICATE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_DIMENSION_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_PARENT_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_SOURCE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_SOURCE_HINT_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_SOURCE_IX_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_SOURCE_IX_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_URL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_URL_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_WB_DIM_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FACT_WRITE_BACK_INFO_API', TRUE);
   Installation_SYS.Remove_Package('XLR_FORMAT_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_IMPORT_LOG_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_INSTANCE_PARAM_VALUE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_ITEM_HINT_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_LOG_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_META_DATA_FILE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_META_DATA_FILE_ENTITY_API', TRUE);
--   Installation_SYS.Remove_Package('XLR_META_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_MV_CRITERIA_API', TRUE);
   Installation_SYS.Remove_Package('XLR_MV_PER_REFRESH_CAT_API', TRUE);
   Installation_SYS.Remove_Package('XLR_MV_REFRESH_CATEGORY_API', TRUE);
   Installation_SYS.Remove_Package('XLR_MV_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_OPERATOR_API', TRUE);
   Installation_SYS.Remove_Package('XLR_PARAMETER_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_PERFORMANCE_INDICATOR_API', TRUE);
   Installation_SYS.Remove_Package('XLR_REPORT_INSTANCE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_REPORT_INSTANCE_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_REPORT_RESULT_API', TRUE);
   Installation_SYS.Remove_Package('XLR_REPORT_UPG_UTILITY_API', TRUE);
   Installation_SYS.Remove_Package('XLR_RESULT_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_SOURCE_NAVIGATOR_API', TRUE);
   Installation_SYS.Remove_Package('XLR_SOURCE_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_STMT_BUILDER_API', TRUE);
   Installation_SYS.Remove_Package('XLR_STMT_EXECUTOR_API', TRUE);
   Installation_SYS.Remove_Package('XLR_STRUCT_REQUEST_API', TRUE);
   Installation_SYS.Remove_Package('XLR_STRUCT_REQUEST_PARENT_API', TRUE);
   Installation_SYS.Remove_Package('XLR_STRUCT_RESPONSE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_STRUCT_RESPONSE_LEAF_API', TRUE);
   Installation_SYS.Remove_Package('XLR_STRUCT_RESPONSE_LEVEL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_STRUCT_RESPONSE_NODE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_STRUCTURE_ITEM_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_STRUCTURE_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_SYS_PARAM_DATA_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_SYSTEM_PARAMETER_API', TRUE);
   Installation_SYS.Remove_Package('XLR_SYSTEM_PARAMETER_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_TEMPLATE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_TEMPLATE_FILE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_TEMPLATE_PARAMETER_API', TRUE);
   Installation_SYS.Remove_Package('XLR_TEMPLATE_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_TRANSLATION_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_VIEW_HANDLING_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WB_COLLECTION_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WB_CONTENT_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WB_FACT_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WB_ITEM_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WB_ROW_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WB_SET_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WB_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WRITE_BACK_CATEGORY_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WRITE_BACK_COLLECTION_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WRITE_BACK_ITEM_DETAIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WRITE_BACK_MASTER_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WRITE_BACK_ROW_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WRITE_BACK_SET_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WRITE_BACK_SET_UQ_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WRITE_BACK_STATUS_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WRITE_BACK_TYPE_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WRITE_BACK_UTIL_API', TRUE);
   Installation_SYS.Remove_Package('XLR_WRITE_BACK_VALUE_API', TRUE);
   Installation_SYS.Remove_Package('XML_RECORD_WRITER_SYS', TRUE);
   Installation_SYS.Remove_Package('XML_REPORT_DATA_API', TRUE);
   Installation_SYS.Remove_Package('XML_SYS', TRUE);
   Installation_SYS.Remove_Package('XML_TEXT_WRITER_API', TRUE);
   /* uncomment only the packages corresponding to the installed components 
   Installation_SYS.Remove_Package('COMPONENT_ACCRUL_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ACCRUW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_AIIM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_APPPAY_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_APPSRV_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_BACLI_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_BENADM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_BENADW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_BOEXP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_BUDPRO_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_BUDPRW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CALBBW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CALLC_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CALLCW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CAREER_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CAREEW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CBS_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CBSW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CFGBKO_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CFGCHR_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CFGRUL_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CHMGMT_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CONACC_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CONMGT_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_COST_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CROMFG_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CRP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_CUSSCH_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_DEMAND_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_DISCOM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_DISORD_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_DOCBB_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_DOCBBW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_DOCMAN_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_DOCMAW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_DOCVUE_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_DOP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ECOMAN_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_EMPINT_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_EMPPAW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_EMPPAY_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_EMPSRV_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_EMPSRW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ENTERP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ENTERW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_EQUBB_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_EQUBBW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_EQUIP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_EQUIPW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ESCHND_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FINCFA_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FINCFW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FINCON_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FINREP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FIXASS_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FMEA_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FNDADM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FNDADS_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FNDBAS_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FNDCOB_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FNDDEV_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FNDJBS_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FNDMIG_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FNDRRE_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_FNDWEB_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_GENLED_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_GENLEW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_HRBASW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_IFSBI_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_INTLED_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_INVENT_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_INVENW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_INVOIC_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_INVOIW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_INVPLA_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ISTOOL_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ITS_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_JINSUI_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_KANBAN_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_KANBAW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_MASSCH_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_MASSCW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_MATCCO_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_MATCCW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_METINV_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_MFGSTD_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_MFGSTW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_MPCCOM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_MROMFG_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_MRP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_MRPW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_MSCOM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_MSCOMW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_NAINFE_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_NATSTD_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ODOCRT_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ODOCTL_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_OEE_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ONLDOC_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ONLPM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ONLTM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ORDBBW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ORDER_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ORDERW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_ORDSTR_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_OSHA_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_OSHAW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PARTCA_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PAYINT_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PAYLED_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PAYLEW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PAYROL_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PCERT_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PCM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PCMBB_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PCMBBW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PCMSCI_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PCMW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PDBCW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PDMCON_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PDMCOW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PDMPRO_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PERCOS_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PERSON_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PERSOW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PLADES_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PLANAV_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PLIST_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PMRP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PMRPW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRBI_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRBSLN_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRDIST_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRDM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PREPLA_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRFIN_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRFND_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRHR_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRIFS_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRJBBW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRJMSP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRJREP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRJREW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRMANU_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRMS_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PROAUT_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PROJ_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PROJW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PROJX_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PROSCH_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PROSCW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRSALS_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PRVIM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PURBBW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PURCH_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_PURCHW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_QUAAUD_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_QUAMAN_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_QUANCR_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_RCCP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_RCRUIT_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_RCRUIW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_RISK_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_RRP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SAMGSS_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SAMIMI_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SAMWIN_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SCEBBW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SCENTR_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SCENTW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SCH360_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SDMAN_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SHPBBW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SHPORD_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SHPORW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SINWOF_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SINWOW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SRVAGR_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SRVCON_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SRVINV_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SRVQUO_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_STRACO_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_STRACW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SUBCON_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SUBVAL_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SUPBBW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SUPKEY_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_SUPSCH_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TAXLED_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TERMS_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TIMCLO_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TIMMAN_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TIMMAW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TIMREP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TIMREW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TRNADM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TRNADW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TRNDEV_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TRNDEW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TRVALL_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TRVALW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TRVEXP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_TRVEXW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_VIM_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_VIMFCA_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_VIMMRO_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_VRTMAP_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_WRKSCH_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_WRKSCW_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_WSCOPE_SYS', TRUE);
   Installation_SYS.Remove_Package('COMPONENT_WSTORE_SYS', TRUE);
   */
END;
/

----------------------------------------------------------------
-- Drop views
----------------------------------------------------------------

BEGIN
   dbms_output.put_line('Removing Views for the Module...');
   Installation_SYS.Remove_View('ACTIVITY_ENTITY_FILTER', TRUE);
   Installation_SYS.Remove_View('ACTIVITY_ENTITY_USAGE', TRUE);
   Installation_SYS.Remove_View('ACTIVITY_FNDDR', TRUE);
   Installation_SYS.Remove_View('ACTIVITY_GRANT', TRUE);
   Installation_SYS.Remove_View('ACTIVITY_GRANT_FILTER', TRUE);
   Installation_SYS.Remove_View('ACTIVITY_PACKAGE', TRUE);
   Installation_SYS.Remove_View('ACTIVITY_PACKAGE_DIAGRAM', TRUE);
   Installation_SYS.Remove_View('ADDRESS_LABEL', TRUE);
   Installation_SYS.Remove_View('ALL_DB_LINKS_LOV', TRUE);
   Installation_SYS.Remove_View('ALLOWED_REPORT', TRUE);
   Installation_SYS.Remove_View('APPLICATION_MESSAGE', TRUE);
   Installation_SYS.Remove_View('ARCHIVE', TRUE);
   Installation_SYS.Remove_View('ARCHIVE_DISTRIBUTION', TRUE);
   Installation_SYS.Remove_View('ARCHIVE_FILE_NAME', TRUE);
   Installation_SYS.Remove_View('ARCHIVE_PARAMETER', TRUE);
   Installation_SYS.Remove_View('ARCHIVE_VARIABLE', TRUE);
   Installation_SYS.Remove_View('ATTRIBUTE_STATE_VALIDATION', TRUE);
   Installation_SYS.Remove_View('ATTRIBUTE_VALIDATION_RULE', TRUE);
   Installation_SYS.Remove_View('AVAILABLE_SEARCH_DOMAINS', TRUE);
   Installation_SYS.Remove_View('BASIC_DATA_TRANSLATION', TRUE);
   Installation_SYS.Remove_View('BASIC_DATA_TRANSLATION_HEAD', TRUE);
   Installation_SYS.Remove_View('BASIC_DATA_TRANSLATION_LOV', TRUE);
   Installation_SYS.Remove_View('BATCH_JOB', TRUE);
   Installation_SYS.Remove_View('BATCH_JOB_LOG', TRUE);
   Installation_SYS.Remove_View('BATCH_QUEUE', TRUE);
   Installation_SYS.Remove_View('BATCH_QUEUE_METHOD', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_CHAIN', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_CHAIN_PAR', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_CHAIN_STEP', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_METHOD', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_METHOD_ALL_PUB', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_METHOD_PAR', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_METHOD_PAR_PUB', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_METHOD_PUB', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_PAR', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_PAR_PUB', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_PUB', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_REPORT', TRUE);
   Installation_SYS.Remove_View('BATCH_SCHEDULE_REPORT_METHOD', TRUE);
   Installation_SYS.Remove_View('BI_TIME_DIMENSION', TRUE);
   Installation_SYS.Remove_View('BINARY_OBJECT', TRUE);
   Installation_SYS.Remove_View('BINARY_OBJECT_DATA_BLOCK', TRUE);
   Installation_SYS.Remove_View('BULLETIN_BOARD_MESSAGES', TRUE);
   Installation_SYS.Remove_View('BULLETIN_BOARD_TOPIC_USERS', TRUE);
   Installation_SYS.Remove_View('BULLETIN_BOARD_TOPICS', TRUE);
   Installation_SYS.Remove_View('CACHE_MANAGEMENT', TRUE);
   Installation_SYS.Remove_View('CACHED_ARTIFACT', TRUE);
   Installation_SYS.Remove_View('CACHED_ARTIFACT_ELEMENT', TRUE);
   Installation_SYS.Remove_View('CLIENT_DIAGRAM', TRUE);
   Installation_SYS.Remove_View('CLIENT_PACKAGE', TRUE);
   Installation_SYS.Remove_View('CLIENT_PLUG_IN', TRUE);
   Installation_SYS.Remove_View('CLIENT_PLUG_IN_ACTIVITY', TRUE);
   Installation_SYS.Remove_View('CLIENT_PROFILE', TRUE);
   Installation_SYS.Remove_View('CLIENT_PROFILE_VALUE', TRUE);
   Installation_SYS.Remove_View('COMMON_MESSAGES', TRUE);
   Installation_SYS.Remove_View('COMPONENT_DEPENDENCY', TRUE);
   Installation_SYS.Remove_View('COMPONENT_FILE_TYPE', TRUE);
   Installation_SYS.Remove_View('COMPONENT_PATCH', TRUE);
   Installation_SYS.Remove_View('COMPONENT_PATCH_ROW', TRUE);
   Installation_SYS.Remove_View('CONDITION_PART', TRUE);
   Installation_SYS.Remove_View('CONFIG_PARAMETER', TRUE);
   Installation_SYS.Remove_View('CONFIG_PARAMETER_AREA', TRUE);
   Installation_SYS.Remove_View('CONFIG_PARAMETER_GROUP', TRUE);
   Installation_SYS.Remove_View('CONFIG_PARAMETER_INSTANCE', TRUE);
   Installation_SYS.Remove_View('CONTEXT_SUBSTITUTION_VAR', TRUE);
   Installation_SYS.Remove_View('CUSTOM_MENU', TRUE);
   Installation_SYS.Remove_View('CUSTOM_MENU_EXP_PAR', TRUE);
   Installation_SYS.Remove_View('CUSTOM_MENU_KEY_TRANS', TRUE);
   Installation_SYS.Remove_View('CUSTOM_MENU_TEXT', TRUE);
   Installation_SYS.Remove_View('DATA_ARCHIVE_COLUMNS_LOV', TRUE);
   Installation_SYS.Remove_View('DATA_ARCHIVE_DBLINK_LOV', TRUE);
   Installation_SYS.Remove_View('DATA_ARCHIVE_EXEC_ATTR', TRUE);
   Installation_SYS.Remove_View('DATA_ARCHIVE_LOG', TRUE);
   Installation_SYS.Remove_View('DATA_ARCHIVE_OBJECT', TRUE);
   Installation_SYS.Remove_View('DATA_ARCHIVE_OBJECT_COLUMNS', TRUE);
   Installation_SYS.Remove_View('DATA_ARCHIVE_ORDER', TRUE);
   Installation_SYS.Remove_View('DATA_ARCHIVE_ORDER_EXEC', TRUE);
   Installation_SYS.Remove_View('DATA_ARCHIVE_RESTORE', TRUE);
   Installation_SYS.Remove_View('DATA_ARCHIVE_SOURCE', TRUE);
   Installation_SYS.Remove_View('DATA_ARCHIVE_SOURCE_ATTR', TRUE);
   Installation_SYS.Remove_View('DATA_ARCHIVE_TABLES_LOV', TRUE);
   Installation_SYS.Remove_View('DB_SCRIPT_REGISTER', TRUE);
   Installation_SYS.Remove_View('DB_SCRIPT_REGISTER_DETAIL', TRUE);
   Installation_SYS.Remove_View('DEFERRED_JOB', TRUE);
   Installation_SYS.Remove_View('DEFERRED_JOB_STATUS', TRUE);
   Installation_SYS.Remove_View('DETAIL_VALIDATION', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_ARGUMENT', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_CONSTRAINTS', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_DOMAIN', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_IND_COLUMNS', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_INDEXES', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_LU', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_METHOD', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_METHOD_LOV', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_PACKAGE', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_STATE', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_STATE_EVENT', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_STATE_MACH', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_STATE_TRANS', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_TAB_COLUMNS', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_TABLES', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_VIEW', TRUE);
   Installation_SYS.Remove_View('DICTIONARY_SYS_VIEW_COLUMN', TRUE);
   Installation_SYS.Remove_View('DIM_BI_TIME_DM', TRUE);
   Installation_SYS.Remove_View('DIM_BI_TIME_OL', TRUE);
   Installation_SYS.Remove_View('DISTRIBUTION_GROUP', TRUE);
   Installation_SYS.Remove_View('DISTRIBUTION_GROUP_MEMBER', TRUE);
   Installation_SYS.Remove_View('DOCUMENT_LIFECYCLE', TRUE);
   Installation_SYS.Remove_View('DRAFT', TRUE);
   Installation_SYS.Remove_View('DSOD', TRUE);
   Installation_SYS.Remove_View('DSOD_CRITERIA', TRUE);
   Installation_SYS.Remove_View('DSOD_DISPLAY_ITEM', TRUE);
   Installation_SYS.Remove_View('DSOD_GROUPING', TRUE);
   Installation_SYS.Remove_View('DSOD_SET', TRUE);
   Installation_SYS.Remove_View('DSOD_SORTING', TRUE);
   Installation_SYS.Remove_View('DSOD_STRUCTURE_REQUEST', TRUE);
   Installation_SYS.Remove_View('ENTITY', TRUE);
   Installation_SYS.Remove_View('ENTITY_ASS_STATE_VALID', TRUE);
   Installation_SYS.Remove_View('ENTITY_ASSOCIATION', TRUE);
   Installation_SYS.Remove_View('ENTITY_ASSOCIATION_ATTR', TRUE);
   Installation_SYS.Remove_View('ENTITY_ATTRIBUTE', TRUE);
   Installation_SYS.Remove_View('ENTITY_DIAGRAM', TRUE);
   Installation_SYS.Remove_View('ENTITY_PACKAGE', TRUE);
   Installation_SYS.Remove_View('ENTITY_PACKAGE_DIAGRAM', TRUE);
   Installation_SYS.Remove_View('ENTITY_STATE', TRUE);
   Installation_SYS.Remove_View('ENUMERATION_VALUE', TRUE);
   Installation_SYS.Remove_View('EXCEL_REPORT_ARCHIVE', TRUE);
   Installation_SYS.Remove_View('FEATURE', TRUE);
   Installation_SYS.Remove_View('FEATURE_ACTIVITY', TRUE);
   Installation_SYS.Remove_View('FEATURE_WIDGET', TRUE);
   Installation_SYS.Remove_View('FILTER', TRUE);
   Installation_SYS.Remove_View('FILTER_PARAMETER', TRUE);
   Installation_SYS.Remove_View('FND_CODE_TEMPLATE', TRUE);
   Installation_SYS.Remove_View('FND_COL_COMMENTS', TRUE);
   Installation_SYS.Remove_View('FND_COMBINED_PDF_ARCHIVE', TRUE);
   Installation_SYS.Remove_View('FND_EVENT', TRUE);
   Installation_SYS.Remove_View('FND_EVENT_ACTION', TRUE);
   Installation_SYS.Remove_View('FND_EVENT_ACTION_SUBSCRIBABLE', TRUE);
   Installation_SYS.Remove_View('FND_EVENT_ACTION_SUBSCRIBE', TRUE);
   Installation_SYS.Remove_View('FND_EVENT_MY_MESSAGES', TRUE);
   Installation_SYS.Remove_View('FND_EVENT_PARAMETER', TRUE);
   Installation_SYS.Remove_View('FND_EVENT_PARAMETER_SPECIAL', TRUE);
   Installation_SYS.Remove_View('FND_GRANT_ROLE', TRUE);
   Installation_SYS.Remove_View('FND_LICENSE', TRUE);
   Installation_SYS.Remove_View('FND_MONITOR_CATEGORY', TRUE);
   Installation_SYS.Remove_View('FND_MONITOR_ENTRY', TRUE);
   Installation_SYS.Remove_View('FND_NOTE_BOOK', TRUE);
   Installation_SYS.Remove_View('FND_NOTE_PAGE', TRUE);
   Installation_SYS.Remove_View('FND_ROLE', TRUE);
   Installation_SYS.Remove_View('FND_ROLE_ROLE', TRUE);
   Installation_SYS.Remove_View('FND_SECURITY_PER_OBJECT_REP', TRUE);
   Installation_SYS.Remove_View('FND_SECURITY_PER_USER_REP', TRUE);
   Installation_SYS.Remove_View('FND_SESSION', TRUE);
   Installation_SYS.Remove_View('FND_SESSION_REP', TRUE);
   Installation_SYS.Remove_View('FND_SESSION_RUNTIME', TRUE);
   Installation_SYS.Remove_View('FND_SETTING', TRUE);
   Installation_SYS.Remove_View('FND_TAB_COMMENTS', TRUE);
   Installation_SYS.Remove_View('FND_TRANSLATION', TRUE);
   Installation_SYS.Remove_View('FND_USER', TRUE);
   Installation_SYS.Remove_View('FND_USER_ERRORS', TRUE);
   Installation_SYS.Remove_View('FND_USER_OBJECTS', TRUE);
   Installation_SYS.Remove_View('FND_USER_ORALOV', TRUE);
   Installation_SYS.Remove_View('FND_USER_PROPERTY', TRUE);
   Installation_SYS.Remove_View('FND_USER_ROLE', TRUE);
   Installation_SYS.Remove_View('FND_USER_ROLE_RUNTIME', TRUE);
   Installation_SYS.Remove_View('FND_USER_SOURCE', TRUE);
   Installation_SYS.Remove_View('FNDCN_MESSAGE_QUEUE', TRUE);
   Installation_SYS.Remove_View('FNDRPL_BO_LU_NAMES', TRUE);
   Installation_SYS.Remove_View('FNDRPL_BO_OBJECT', TRUE);
   Installation_SYS.Remove_View('FNDRPL_BO_PACKAGE', TRUE);
   Installation_SYS.Remove_View('FNDRPL_BO_TRIGGER', TRUE);
   Installation_SYS.Remove_View('FNDRPL_BUSINESS_OBJECT', TRUE);
   Installation_SYS.Remove_View('FNDRPL_COMPONENTS', TRUE);
   Installation_SYS.Remove_View('FNDRPL_LU_NAMES', TRUE);
   Installation_SYS.Remove_View('FNDRPL_RECEIVER', TRUE);
   Installation_SYS.Remove_View('FNDRPL_RG_COLUMN_NAME', TRUE);
   Installation_SYS.Remove_View('FNDRPL_RG_TABLE_NAME', TRUE);
   Installation_SYS.Remove_View('FNDRPL_RO_LU_NAMES', TRUE);
   Installation_SYS.Remove_View('FNDRR_CLIENT_PROFILE', TRUE);
   Installation_SYS.Remove_View('FNDRR_CLIENT_PROFILE_VALUE', TRUE);
   Installation_SYS.Remove_View('FNDRR_USER_CLIENT_PROFILE', TRUE);
   Installation_SYS.Remove_View('FUNC_AREA_CONFLICT_CACHE', TRUE);
   Installation_SYS.Remove_View('FUNC_AREA_CONFLICT_PERMISSIONS', TRUE);
   Installation_SYS.Remove_View('FUNC_AREA_SEC_CACHE', TRUE);
   Installation_SYS.Remove_View('FUNC_AREA_USER_PERMISSIONS', TRUE);
   Installation_SYS.Remove_View('FUNCTIONAL_AREA_ACTIVITY', TRUE);
   Installation_SYS.Remove_View('FUNCTIONAL_AREA_CONFLICT', TRUE);
   Installation_SYS.Remove_View('FUNCTIONAL_AREA_CONFLICT_REP', TRUE);
   Installation_SYS.Remove_View('FUNCTIONAL_AREA_METHOD', TRUE);
   Installation_SYS.Remove_View('FUNCTIONAL_AREA_VIEW', TRUE);
   Installation_SYS.Remove_View('HANDLER', TRUE);
   Installation_SYS.Remove_View('HANDLER_METHOD', TRUE);
   Installation_SYS.Remove_View('HISTORY_LOG', TRUE);
   Installation_SYS.Remove_View('HISTORY_LOG_ADMIN', TRUE);
   Installation_SYS.Remove_View('HISTORY_LOG_ATTRIBUTE', TRUE);
   Installation_SYS.Remove_View('HISTORY_LOG_REP', TRUE);
   Installation_SYS.Remove_View('HISTORY_SETTING', TRUE);
   Installation_SYS.Remove_View('HISTORY_SETTING_ATTRIBUTE', TRUE);
   Installation_SYS.Remove_View('I_FACE_CONTROL_PARAMETER', TRUE);
   Installation_SYS.Remove_View('IAL_OBJECT', TRUE);
   Installation_SYS.Remove_View('IN_MESSAGE', TRUE);
   Installation_SYS.Remove_View('IN_MESSAGE_LINE', TRUE);
   Installation_SYS.Remove_View('IN_MESSAGE_LOADED', TRUE);
   Installation_SYS.Remove_View('INDEX_COLUMN', TRUE);
   Installation_SYS.Remove_View('INFO_SERVICES_RPV', TRUE);
   Installation_SYS.Remove_View('INSTALLATION_SITE', TRUE);
   Installation_SYS.Remove_View('ITEM_OWNER', TRUE);
   Installation_SYS.Remove_View('J2EE_APPLICATION', TRUE);
   Installation_SYS.Remove_View('J2EE_MODULE', TRUE);
   Installation_SYS.Remove_View('LANG_CODE_RFC3066', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_ATTRIBUTE', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_CODE', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_CODE_DISTINCT', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_CONNECTION', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_CONTENT_TYPE', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_CONTEXT', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_DESTINATION', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_FILE_EXPORT', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_FILE_IMPORT', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_FONT_MAPPING', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_MODULE', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_PROPERTY', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_SOURCE', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_SYS_IMP', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_TR_STAT_CTX', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_TR_STAT_OVERVIEW', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_TRANSLATION', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_TRANSLATION_EXP', TRUE);
   Installation_SYS.Remove_View('LANGUAGE_TRANSLATION_LOC', TRUE);
   Installation_SYS.Remove_View('LDAP_CONFIGURATION', TRUE);
   Installation_SYS.Remove_View('LDAP_DOMAIN_CONFIG', TRUE);
   Installation_SYS.Remove_View('LDAP_MAPPING', TRUE);
   Installation_SYS.Remove_View('LOGICAL_PRINTER', TRUE);
   Installation_SYS.Remove_View('LOGICAL_UNIT', TRUE);
   Installation_SYS.Remove_View('LOGICAL_UNIT_DETAILS', TRUE);
   Installation_SYS.Remove_View('LU_ATTRIBUTE', TRUE);
   Installation_SYS.Remove_View('LU_OPERATION', TRUE);
   Installation_SYS.Remove_View('MAPPED_ENTITY', TRUE);
   Installation_SYS.Remove_View('MESSAGE_ARCHIVE', TRUE);
   Installation_SYS.Remove_View('MESSAGE_ARCHIVE_ADDRESS', TRUE);
   Installation_SYS.Remove_View('MESSAGE_ARCHIVE_BODY', TRUE);
   Installation_SYS.Remove_View('MESSAGE_ARCHIVE_SEARCH', TRUE);
   Installation_SYS.Remove_View('MESSAGE_BODY', TRUE);
   Installation_SYS.Remove_View('MESSAGE_CLASS', TRUE);
   Installation_SYS.Remove_View('MESSAGE_MEDIA', TRUE);
   Installation_SYS.Remove_View('MESSAGE_QUEUE', TRUE);
   Installation_SYS.Remove_View('MESSAGE_RECEIVER', TRUE);
   Installation_SYS.Remove_View('METHOD_FILTER', TRUE);
   Installation_SYS.Remove_View('METHOD_PARAMETER', TRUE);
   Installation_SYS.Remove_View('MOBILE_CLIENT_PACKAGE', TRUE);
   Installation_SYS.Remove_View('MOBILE_ENTITY_SYNCH', TRUE);
   Installation_SYS.Remove_View('MOBILE_OPTIMIZER_DATA', TRUE);
   Installation_SYS.Remove_View('MOBILE_USER_CACHE', TRUE);
   Installation_SYS.Remove_View('MODEL_IMPORT_LOG', TRUE);
   Installation_SYS.Remove_View('MODEL_WORKSPACE', TRUE);
   Installation_SYS.Remove_View('MODULE', TRUE);
   Installation_SYS.Remove_View('MODULE_DB_PATCH', TRUE);
   Installation_SYS.Remove_View('MODULE_DEPENDENCY', TRUE);
   Installation_SYS.Remove_View('MODULE_DIAGRAM', TRUE);
   Installation_SYS.Remove_View('MODULE_FNDDR', TRUE);
   Installation_SYS.Remove_View('MODULE_INSTALLED', TRUE);
   Installation_SYS.Remove_View('MODULE_LU_LOV', TRUE);
   Installation_SYS.Remove_View('MODULE_REP', TRUE);
   Installation_SYS.Remove_View('MODULE_SYSTEM_DOC', TRUE);
   Installation_SYS.Remove_View('MODULE_TRANSLATION_PUB', TRUE);
   Installation_SYS.Remove_View('MY_TODO_ITEM', TRUE);
   Installation_SYS.Remove_View('NOTE', TRUE);
   Installation_SYS.Remove_View('NOTE_BOOK', TRUE);
   Installation_SYS.Remove_View('OBJECT_CONNECTION', TRUE);
   Installation_SYS.Remove_View('ORACLE_ACCOUNT', TRUE);
   Installation_SYS.Remove_View('ORACLE_ACCOUNT_TABLESPACE', TRUE);
   Installation_SYS.Remove_View('ORACLE_ALERT_LOG', TRUE);
   Installation_SYS.Remove_View('ORACLE_CHARACTER_SET_LOV', TRUE);
   Installation_SYS.Remove_View('ORACLE_INDEXES', TRUE);
   Installation_SYS.Remove_View('ORACLE_LOB_EXTENTS', TRUE);
   Installation_SYS.Remove_View('ORACLE_PROFILE', TRUE);
   Installation_SYS.Remove_View('ORACLE_PROFILE_LIMITS', TRUE);
   Installation_SYS.Remove_View('ORACLE_USER_ERRORS', TRUE);
   Installation_SYS.Remove_View('ORACLE_USER_INDEXES', TRUE);
   Installation_SYS.Remove_View('ORACLE_USER_OBJECTS', TRUE);
   Installation_SYS.Remove_View('ORACLE_USER_SOURCE', TRUE);
   Installation_SYS.Remove_View('ORACLE_USER_TABLES', TRUE);
   Installation_SYS.Remove_View('OUT_MESSAGE', TRUE);
   Installation_SYS.Remove_View('OUT_MESSAGE_LINE', TRUE);
   Installation_SYS.Remove_View('PACKAGE_DEPENDENCY', TRUE);
   Installation_SYS.Remove_View('PACKAGE_DIAGRAM', TRUE);
   Installation_SYS.Remove_View('PARAMETER', TRUE);
   Installation_SYS.Remove_View('PDF_ARCHIVE', TRUE);
   Installation_SYS.Remove_View('PERFORMANCE_ANALYZE_SAVED_SRC', TRUE);
   Installation_SYS.Remove_View('PERFORMANCE_ANALYZE_SOURCE', TRUE);
   Installation_SYS.Remove_View('PERMISSION_SET_FILTER', TRUE);
   Installation_SYS.Remove_View('PERMISSION_SET_FILTER_PAR', TRUE);
   Installation_SYS.Remove_View('PLSQL_METHOD', TRUE);
   Installation_SYS.Remove_View('PLSQL_PACKAGE', TRUE);
   Installation_SYS.Remove_View('PLSQL_PARAMETER', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_BUILD', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_CHANGE', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_DEP_CHANGE', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_DEPENDENCY', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_DESCRIPTION', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_DIC_SECURITY', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_EXCLUDE', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_GRANT', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_INCLUDE', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_INCLUDE_SEC', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_SEC_CHANGE', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_SEC_EXPORT', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_SECURITY', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_SECURITY_AVAIL', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_SECURITY_BUILD', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_SECURITY_TYPE', TRUE);
   Installation_SYS.Remove_View('PRES_OBJECT_TYPE', TRUE);
   Installation_SYS.Remove_View('PRINT_JOB', TRUE);
   Installation_SYS.Remove_View('PRINT_JOB_CONTENTS', TRUE);
   Installation_SYS.Remove_View('PRINT_QUEUE', TRUE);
   Installation_SYS.Remove_View('PROJECT_SETTING', TRUE);
   Installation_SYS.Remove_View('QUERY_HINT_COL', TRUE);
   Installation_SYS.Remove_View('QUERY_HINT_INDEX', TRUE);
   Installation_SYS.Remove_View('QUERY_HINT_INDEX_LOV', TRUE);
   Installation_SYS.Remove_View('QUERY_HINT_TABLE', TRUE);
   Installation_SYS.Remove_View('QUERY_HINT_VIEW', TRUE);
   Installation_SYS.Remove_View('QUICK_REPORT', TRUE);
   Installation_SYS.Remove_View('QUICK_REPORT_NON_BI', TRUE);
   Installation_SYS.Remove_View('RECEIVE_ATTR', TRUE);
   Installation_SYS.Remove_View('RECEIVE_ATTR_GROUP', TRUE);
   Installation_SYS.Remove_View('RECEIVE_CONDITION', TRUE);
   Installation_SYS.Remove_View('RECEIVE_GROUP', TRUE);
   Installation_SYS.Remove_View('RECEIVE_OBJECT', TRUE);
   Installation_SYS.Remove_View('RECURRENCE_AGENDA', TRUE);
   Installation_SYS.Remove_View('RECURRENCE_PATTERN', TRUE);
   Installation_SYS.Remove_View('REMOTE_PRINTER_MAPPING', TRUE);
   Installation_SYS.Remove_View('REMOTE_PRINTING_NODE', TRUE);
   Installation_SYS.Remove_View('REPLICATION_ATTR_DEF', TRUE);
   Installation_SYS.Remove_View('REPLICATION_ATTR_GROUP', TRUE);
   Installation_SYS.Remove_View('REPLICATION_ATTR_GROUP_DEF', TRUE);
   Installation_SYS.Remove_View('REPLICATION_CONDITION', TRUE);
   Installation_SYS.Remove_View('REPLICATION_GROUP', TRUE);
   Installation_SYS.Remove_View('REPLICATION_LOG', TRUE);
   Installation_SYS.Remove_View('REPLICATION_OBJECT', TRUE);
   Installation_SYS.Remove_View('REPLICATION_OBJECT_DEF', TRUE);
   Installation_SYS.Remove_View('REPLICATION_QUEUE', TRUE);
   Installation_SYS.Remove_View('REPLICATION_RECEIVER', TRUE);
   Installation_SYS.Remove_View('REPLICATION_SENDER', TRUE);
   Installation_SYS.Remove_View('REPLICATION_STATISTICS', TRUE);
   Installation_SYS.Remove_View('REPORT_CATEGORY', TRUE);
   Installation_SYS.Remove_View('REPORT_CATEGORY_LOV', TRUE);
   Installation_SYS.Remove_View('REPORT_COLUMN_DEFINITION', TRUE);
   Installation_SYS.Remove_View('REPORT_DEFINITION', TRUE);
   Installation_SYS.Remove_View('REPORT_DEFINITION_USER', TRUE);
   Installation_SYS.Remove_View('REPORT_FONT_DEFINITION', TRUE);
   Installation_SYS.Remove_View('REPORT_LAYOUT', TRUE);
   Installation_SYS.Remove_View('REPORT_LAYOUT_DEFINITION', TRUE);
   Installation_SYS.Remove_View('REPORT_LAYOUT_TYPE_CONFIG', TRUE);
   Installation_SYS.Remove_View('REPORT_LU_DEFINITION', TRUE);
   Installation_SYS.Remove_View('REPORT_PARAMETER', TRUE);
   Installation_SYS.Remove_View('REPORT_PDF_INSERT', TRUE);
   Installation_SYS.Remove_View('REPORT_PLUGIN', TRUE);
   Installation_SYS.Remove_View('REPORT_RESULT_GEN_CONFIG', TRUE);
   Installation_SYS.Remove_View('REPORT_RULE_ACTION', TRUE);
   Installation_SYS.Remove_View('REPORT_RULE_CONDITION', TRUE);
   Installation_SYS.Remove_View('REPORT_RULE_DEFINITION', TRUE);
   Installation_SYS.Remove_View('REPORT_RULE_LOG', TRUE);
   Installation_SYS.Remove_View('REPORT_SCHEMA', TRUE);
   Installation_SYS.Remove_View('REPORT_TEXT', TRUE);
   Installation_SYS.Remove_View('REPORT_USER_SETTINGS', TRUE);
   Installation_SYS.Remove_View('ROUTE_ADDRESS', TRUE);
   Installation_SYS.Remove_View('ROUTE_ADDRESS_REFERENCE', TRUE);
   Installation_SYS.Remove_View('ROUTE_CONDITION', TRUE);
   Installation_SYS.Remove_View('RUNTIME_TRANSLATIONS', TRUE);
   Installation_SYS.Remove_View('SEARCH_DOMAIN_DOCUMENT', TRUE);
   Installation_SYS.Remove_View('SEARCH_DOMAIN_ERRORS', TRUE);
   Installation_SYS.Remove_View('SEARCH_DOMAIN_GROUP', TRUE);
   Installation_SYS.Remove_View('SEARCH_DOMAIN_GROUP_MEMBER', TRUE);
   Installation_SYS.Remove_View('SEARCH_DOMAIN_PENDING_DOCUMENT', TRUE);
   Installation_SYS.Remove_View('SEARCH_DOMAIN_RUNTIME', TRUE);
   Installation_SYS.Remove_View('SEARCH_DOMAIN_UNSYNCHED', TRUE);
   Installation_SYS.Remove_View('SEC_CHECKPOINT_GATE', TRUE);
   Installation_SYS.Remove_View('SEC_CHECKPOINT_GATE_PARAM', TRUE);
   Installation_SYS.Remove_View('SEC_CHECKPOINT_LOG', TRUE);
   Installation_SYS.Remove_View('SECURITY_SYS_DIC_GRANTED_OBJ', TRUE);
   Installation_SYS.Remove_View('SECURITY_SYS_GRANTED_IALS', TRUE);
   Installation_SYS.Remove_View('SECURITY_SYS_GRANTED_OBJECTS', TRUE);
   Installation_SYS.Remove_View('SECURITY_SYS_REVOKED_METHODS', TRUE);
   Installation_SYS.Remove_View('SECURITY_SYS_ROLE_TREE', TRUE);
   Installation_SYS.Remove_View('SECURITY_SYS_USER_ROLE_LIST', TRUE);
   Installation_SYS.Remove_View('SECURITY_SYS_USER_ROLE_TMP', TRUE);
   Installation_SYS.Remove_View('SELECTION', TRUE);
   Installation_SYS.Remove_View('SELECTION_ITEM', TRUE);
   Installation_SYS.Remove_View('SERV_PACKAGE_DEPENDENCY', TRUE);
   Installation_SYS.Remove_View('SERVER_LOG', TRUE);
   Installation_SYS.Remove_View('SERVER_LOG_CATEGORY', TRUE);
   Installation_SYS.Remove_View('SERVER_PACKAGE', TRUE);
   Installation_SYS.Remove_View('SITE_TEXT', TRUE);
   Installation_SYS.Remove_View('SOD_CACHE', TRUE);
   Installation_SYS.Remove_View('SOX_FUNCTIONAL_AREA', TRUE);
   Installation_SYS.Remove_View('STATE_DIAGRAM', TRUE);
   Installation_SYS.Remove_View('STATE_EVENT', TRUE);
   Installation_SYS.Remove_View('STEREOTYPE', TRUE);
   Installation_SYS.Remove_View('STORAGE_ATTRIBUTE', TRUE);
   Installation_SYS.Remove_View('STORAGE_DIAGRAM', TRUE);
   Installation_SYS.Remove_View('STORAGE_OBJECT', TRUE);
   Installation_SYS.Remove_View('STORAGE_PACKAGE', TRUE);
   Installation_SYS.Remove_View('SYSTEM_PRIVILEGE', TRUE);
   Installation_SYS.Remove_View('SYSTEM_PRIVILEGE_GRANT', TRUE);
   Installation_SYS.Remove_View('TABLE_INDEX', TRUE);
   Installation_SYS.Remove_View('TARGET', TRUE);
   Installation_SYS.Remove_View('TEMPLATE', TRUE);
   Installation_SYS.Remove_View('TERM', TRUE);
   Installation_SYS.Remove_View('TERM_ALERT_INDICATOR', TRUE);
   Installation_SYS.Remove_View('TERM_DEFINITION_HISTORY', TRUE);
   Installation_SYS.Remove_View('TERM_DISPLAY_NAME', TRUE);
   Installation_SYS.Remove_View('TERM_DOMAIN', TRUE);
   Installation_SYS.Remove_View('TERM_HISTORY', TRUE);
   Installation_SYS.Remove_View('TERM_OWNER', TRUE);
   Installation_SYS.Remove_View('TERM_OWNER_MODULE', TRUE);
   Installation_SYS.Remove_View('TERM_RELATION', TRUE);
   Installation_SYS.Remove_View('TERM_STOP_LIST', TRUE);
   Installation_SYS.Remove_View('TERM_TRANSLATED_DEFINITION', TRUE);
   Installation_SYS.Remove_View('TERM_TRANSLATED_NAME', TRUE);
   Installation_SYS.Remove_View('TERM_USAGE_DEFINITION', TRUE);
   Installation_SYS.Remove_View('TERM_USAGE_GET_ALL_TEXTS', TRUE);
   Installation_SYS.Remove_View('TERM_USAGE_IDENTIFIER', TRUE);
   Installation_SYS.Remove_View('TODO_FOLDER', TRUE);
   Installation_SYS.Remove_View('TODO_HISTORY', TRUE);
   Installation_SYS.Remove_View('TODO_ITEM', TRUE);
   Installation_SYS.Remove_View('TODO_ITEM_RECEIVER', TRUE);
   Installation_SYS.Remove_View('UML_DIAGRAM', TRUE);
   Installation_SYS.Remove_View('USER_AREA_ALL_PERMISSIONS', TRUE);
   Installation_SYS.Remove_View('USER_CLIENT_PROFILE', TRUE);
   Installation_SYS.Remove_View('USER_DB_LINKS_LOV', TRUE);
   Installation_SYS.Remove_View('USER_FUNC_NO_CONFLICT_AREAS', TRUE);
   Installation_SYS.Remove_View('USER_FUNCTIONAL_AREA_CONFLICTS', TRUE);
   Installation_SYS.Remove_View('USER_GROUP', TRUE);
   Installation_SYS.Remove_View('USER_GROUP_USER', TRUE);
   Installation_SYS.Remove_View('USER_PROFILE', TRUE);
   Installation_SYS.Remove_View('USER_PROFILE_ENTRY', TRUE);
   Installation_SYS.Remove_View('VALIDATION_OBJECT', TRUE);
   Installation_SYS.Remove_View('VIEW_ASSOCIATION', TRUE);
   Installation_SYS.Remove_View('VIEW_ATTRIBUTE', TRUE);
   Installation_SYS.Remove_View('VIEW_FNDDR', TRUE);
   Installation_SYS.Remove_View('WIDGET', TRUE);
   Installation_SYS.Remove_View('WIDGET_ACTIVITY', TRUE);
   Installation_SYS.Remove_View('WORKSPACE', TRUE);
   Installation_SYS.Remove_View('XLR_ANALYZE_RESULT_UTIL', TRUE);
   Installation_SYS.Remove_View('XLR_BA_CLIENT_ARCHIVE', TRUE);
   Installation_SYS.Remove_View('XLR_BA_CLIENT_ARCHIVE_DIST', TRUE);
   Installation_SYS.Remove_View('XLR_BI_USED_MVIEWS', TRUE);
   Installation_SYS.Remove_View('XLR_CONNECTED_MVIEWS', TRUE);
   Installation_SYS.Remove_View('XLR_DATA_MART_VIEWS', TRUE);
   Installation_SYS.Remove_View('XLR_DEF_FACT_NAV_FOLDER', TRUE);
   Installation_SYS.Remove_View('XLR_DIM_FACT_DIRECT_REF_MVIEWS', TRUE);
   Installation_SYS.Remove_View('XLR_DIM_ITEM_STRUCT_INFO', TRUE);
   Installation_SYS.Remove_View('XLR_DIM_REF_RELATIONS', TRUE);
   Installation_SYS.Remove_View('XLR_DIM_SOURCE_HINT_ITEM', TRUE);
   Installation_SYS.Remove_View('XLR_DIM_SOURCE_HINT_ITEM_VIEW', TRUE);
   Installation_SYS.Remove_View('XLR_DIM_SOURCE_IX', TRUE);
   Installation_SYS.Remove_View('XLR_DIM_SOURCE_IX_ITEM', TRUE);
   Installation_SYS.Remove_View('XLR_DIM_VIEW_INFO', TRUE);
   Installation_SYS.Remove_View('XLR_DIMENSION', TRUE);
   Installation_SYS.Remove_View('XLR_DIMENSION_ITEM', TRUE);
   Installation_SYS.Remove_View('XLR_DIMENSION_LOV_ITEM', TRUE);
   Installation_SYS.Remove_View('XLR_DIMENSION_LOV_SORT', TRUE);
   Installation_SYS.Remove_View('XLR_DIMENSION_PARENT', TRUE);
   Installation_SYS.Remove_View('XLR_DIMENSION_SOURCE', TRUE);
   Installation_SYS.Remove_View('XLR_DIMENSIONS', TRUE);
   Installation_SYS.Remove_View('XLR_DIRECT_REF_MVIEWS', TRUE);
   Installation_SYS.Remove_View('XLR_EXEC_ORDERABLE_TEMPLATE', TRUE);
   Installation_SYS.Remove_View('XLR_FACT', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_DETAIL_DIM_ITEM', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_DIM_ID_DEF', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_DIM_JOIN', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_DIM_JOIN_ITEM', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_DIM_SRC_INDICATE', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_DIMENSION', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_ITEM', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_PARENT', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_SOURCE', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_SOURCE_HINT_ITEM', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_SOURCE_IX', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_SOURCE_IX_ITEM', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_URL', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_URL_ITEM', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_WB_DIM_ITEM', TRUE);
   Installation_SYS.Remove_View('XLR_FACT_WRITE_BACK_INFO', TRUE);
   Installation_SYS.Remove_View('XLR_FACTS', TRUE);
   Installation_SYS.Remove_View('XLR_IMPORT_LOG', TRUE);
   Installation_SYS.Remove_View('XLR_INSTANCE_PARAM_VALUE', TRUE);
   Installation_SYS.Remove_View('XLR_META_DATA_FILE_ENTITY', TRUE);
   Installation_SYS.Remove_View('XLR_MODULES', TRUE);
   Installation_SYS.Remove_View('XLR_MV_CRITERIA', TRUE);
   Installation_SYS.Remove_View('XLR_MV_INFO', TRUE);
   Installation_SYS.Remove_View('XLR_MV_PER_REFRESH_CAT', TRUE);
   Installation_SYS.Remove_View('XLR_MV_REFRESH_CATEGORY', TRUE);
   Installation_SYS.Remove_View('XLR_MVIEW_LOGS', TRUE);
   Installation_SYS.Remove_View('XLR_MVIEWS', TRUE);
   Installation_SYS.Remove_View('XLR_REFERENCED_MVIEWS', TRUE);
   Installation_SYS.Remove_View('XLR_REPORT_INSTANCE', TRUE);
   Installation_SYS.Remove_View('XLR_REPORT_INSTANCE_UTIL', TRUE);
   Installation_SYS.Remove_View('XLR_REPORT_RESULT', TRUE);
   Installation_SYS.Remove_View('XLR_REPORT_UPG_UTILITY', TRUE);
   Installation_SYS.Remove_View('XLR_REPORT_UPG_UTILITY_BASE', TRUE);
   Installation_SYS.Remove_View('XLR_RUNTIME_LOG', TRUE);
   Installation_SYS.Remove_View('XLR_SOURCE_NAVIGATOR', TRUE);
   Installation_SYS.Remove_View('XLR_SOURCES', TRUE);
   Installation_SYS.Remove_View('XLR_STRUCT_REQUEST', TRUE);
   Installation_SYS.Remove_View('XLR_STRUCT_REQUEST_PARENT', TRUE);
   Installation_SYS.Remove_View('XLR_STRUCT_RESPONSE', TRUE);
   Installation_SYS.Remove_View('XLR_STRUCT_RESPONSE_LEAF', TRUE);
   Installation_SYS.Remove_View('XLR_STRUCT_RESPONSE_LEVEL', TRUE);
   Installation_SYS.Remove_View('XLR_STRUCT_RESPONSE_NODE', TRUE);
   Installation_SYS.Remove_View('XLR_STRUCTURE_UTIL', TRUE);
   Installation_SYS.Remove_View('XLR_SYSTEM_PARAMETER', TRUE);
   Installation_SYS.Remove_View('XLR_TABLE_COLUMNS', TRUE);
   Installation_SYS.Remove_View('XLR_TEMPLATE', TRUE);
   Installation_SYS.Remove_View('XLR_TEMPLATE_FILE', TRUE);
   Installation_SYS.Remove_View('XLR_TEMPLATE_PARAMETER', TRUE);
   Installation_SYS.Remove_View('XLR_TEMPLATE_UTIL', TRUE);
   Installation_SYS.Remove_View('XLR_TRANSLATION_SOURCE', TRUE);
   Installation_SYS.Remove_View('XLR_USED_DIMENSIONS', TRUE);
   Installation_SYS.Remove_View('XLR_USER_MVIEWS', TRUE);
   Installation_SYS.Remove_View('XLR_WB_COLLECTION', TRUE);
   Installation_SYS.Remove_View('XLR_WB_COLLECTION_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_WB_CONTENT', TRUE);
   Installation_SYS.Remove_View('XLR_WB_FACT', TRUE);
   Installation_SYS.Remove_View('XLR_WB_ITEM', TRUE);
   Installation_SYS.Remove_View('XLR_WB_ITEM_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_WB_ROW', TRUE);
   Installation_SYS.Remove_View('XLR_WB_ROW_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_WB_SECURITY_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_WB_SET', TRUE);
   Installation_SYS.Remove_View('XLR_WB_SET_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_COLLECTION', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_DETAIL_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_ITEM_DETAIL', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_ITEM_DETAIL_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_MASTER', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_MASTER_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_ROW', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_ROW_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_SECURITY_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_SET', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_SET_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_SET_UQ', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_SET_UQ_PUB', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_UTIL', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_VALUE', TRUE);
   Installation_SYS.Remove_View('XLR_WRITE_BACK_VALUE_PUB', TRUE);
   Installation_SYS.Remove_View('XML_REPORT_DATA', TRUE);
END;
/

----------------------------------------------------------------
-- Drop triggers
----------------------------------------------------------------

BEGIN
   dbms_output.put_line('Removing Triggers for the Module...');
   Installation_SYS.Remove_Trigger('FND_USER_SDD1', TRUE);
   Installation_SYS.Remove_Trigger('FND_USER_SDD2', TRUE);
   Installation_SYS.Remove_Trigger('FND_USER_SDI1', TRUE);
   Installation_SYS.Remove_Trigger('FND_USER_SDI2', TRUE);
   Installation_SYS.Remove_Trigger('FND_USER_SDU', TRUE);
   Installation_SYS.Remove_Trigger('FND_USER_SDU1', TRUE);
   Installation_SYS.Remove_Trigger('Message_Archive_Util_RBT', TRUE);
   Installation_SYS.Remove_Trigger('TERM_SDD1', TRUE);
   Installation_SYS.Remove_Trigger('TERM_SDI1', TRUE);
   Installation_SYS.Remove_Trigger('TERM_SDU', TRUE);
   Installation_SYS.Remove_Trigger('USER_PROFILE_SDD1', TRUE);
   Installation_SYS.Remove_Trigger('USER_PROFILE_SDI1', TRUE);
   Installation_SYS.Remove_Trigger('USER_PROFILE_SDU', TRUE);
   Installation_SYS.Remove_Trigger('USER_PROFILE_SDU1', TRUE);
END;
/

----------------------------------------------------------------
-- Drop sequences
----------------------------------------------------------------

BEGIN
   dbms_output.put_line('Removing Sequences Refered only by the current Module...');
   Installation_SYS.Remove_Sequence('BINARY_OBJECT_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('BULLETIN_BOARD_TOPIC_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('COMMON_MESSAGES_ID_SEQUENCE', TRUE);
   Installation_SYS.Remove_Sequence('COMPONENT_DEPENDENCY_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('CUSTOM_MENU_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('FNDCN_MESSAGE_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('FND_EVENT_MY_MESSAGES_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('HISTORY_LOG_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('IN_MESSAGE_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('JOBSEQ', TRUE);
   Installation_SYS.Remove_Sequence('LANGUAGE_ATTRIBUTE_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('LANGUAGE_CONTEXT_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('OUT_MESSAGE_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('PLSQLAP_BUFFER_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('PRES_OBJECT_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('PRINT_JOB_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('REPLICATION_LOG_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('REPLICATION_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('REPORT_CATEGORY_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('REPORT_RULE_CONDITION_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('SCHEDULE_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('SCHEDULE_METHOD_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('SEC_CHECKPOINT_LOG_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('TRANSACTION_SYS_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_BA_CLIENT_ARCHIVE_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_IMPORT_LOG_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_LOG_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_METADATA_CONF_LOG_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_REPORT_RESULT_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_REPORT_RESULT_INST_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_SOURCE_NAVIGATOR_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_STRUCTURE_REQUEST_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_STRUCTURE_RESPONSE_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_TEMPLATE_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_WB_COLLECTION_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_WB_MASTER_ID_SEQ', TRUE);
   Installation_SYS.Remove_Sequence('XLR_WRITE_BACK_MASTER_ID_SEQ', TRUE);
END;
/

----------------------------------------------------------------
-- NOTE: Sequences below are refered by current module but are also
--       referred by other packages in other modules
----------------------------------------------------------------

BEGIN
   dbms_output.put_line('Removing Sequences that are referred by other Modules as well...');
   NULL;
END;
/

----------------------------------------------------------------
-- Drop Materialized Views
----------------------------------------------------------------

BEGIN
   dbms_output.put_line('Removing Materialized Views for the Module...');
   NULL;
END;
/

----------------------------------------------------------------
-- Drop tables
----------------------------------------------------------------
-- NOTE: Tables below are referred only by current module...

BEGIN
   dbms_output.put_line('Removing Tables refered only by current Module...');
   Installation_SYS.Remove_Table('ACTIVITY_GRANT_FILTER_TAB', TRUE);
   Installation_SYS.Remove_Table('ACTIVITY_GRANT_TAB', TRUE);
   Installation_SYS.Remove_Table('ARCHIVE_DISTRIBUTION_TAB', TRUE);
   Installation_SYS.Remove_Table('ARCHIVE_FILE_NAME_TAB', TRUE);
   Installation_SYS.Remove_Table('ARCHIVE_PARAMETER_TAB', TRUE);
   Installation_SYS.Remove_Table('ARCHIVE_VARIABLE_TAB', TRUE);
   Installation_SYS.Remove_Table('BATCH_QUEUE_METHOD_TAB', TRUE);
   Installation_SYS.Remove_Table('BATCH_QUEUE_TAB', TRUE);
   Installation_SYS.Remove_Table('BATCH_SCHEDULE_CHAIN_PAR_TAB', TRUE);
   Installation_SYS.Remove_Table('BATCH_SCHEDULE_CHAIN_STEP_TAB', TRUE);
   Installation_SYS.Remove_Table('BATCH_SCHEDULE_CHAIN_TAB', TRUE);
   Installation_SYS.Remove_Table('BATCH_SCHEDULE_METHOD_PAR_TAB', TRUE);
   Installation_SYS.Remove_Table('BATCH_SYS_TAB', TRUE);
   Installation_SYS.Remove_Table('BATCH_SCHEDULE_TAB', TRUE);
   Installation_SYS.Remove_Table('BINARY_OBJECT_DATA_BLOCK_TAB', TRUE);
   Installation_SYS.Remove_Table('BINARY_OBJECT_TAB', TRUE);
   Installation_SYS.Remove_Table('BULLETIN_BOARD_MESSAGES_TAB', TRUE);
   Installation_SYS.Remove_Table('BULLETIN_BOARD_TOPIC_USERS_TAB', TRUE);
   Installation_SYS.Remove_Table('BULLETIN_BOARD_TOPICS_TAB', TRUE);
   Installation_SYS.Remove_Table('CACHE_MANAGEMENT_TAB', TRUE);
   Installation_SYS.Remove_Table('CLEANUP_REPORT_DATA_TEMP', TRUE);
   Installation_SYS.Remove_Table('CLEANUP_TEMPLATE_DATA_TEMP', TRUE);
   Installation_SYS.Remove_Table('COMMON_MESSAGES_TAB', TRUE);
   Installation_SYS.Remove_Table('COMPONENT_DEPENDENCY_TAB', TRUE);
   Installation_SYS.Remove_Table('COMPONENT_FILE_TYPE_TAB', TRUE);
   Installation_SYS.Remove_Table('COMPONENT_PATCH_ROW_TAB', TRUE);
   Installation_SYS.Remove_Table('COMPONENT_PATCH_TAB', TRUE);
   Installation_SYS.Remove_Table('CONTEXT_SUBSTITUTION_VAR_TAB', TRUE);
   Installation_SYS.Remove_Table('CUSTOM_MENU_EXP_PAR_TAB', TRUE);
   Installation_SYS.Remove_Table('CUSTOM_MENU_KEY_TRANS_TAB', TRUE);
   Installation_SYS.Remove_Table('CUSTOM_MENU_TAB', TRUE);
   Installation_SYS.Remove_Table('CUSTOM_MENU_TEXT_TAB', TRUE);
   Installation_SYS.Remove_Table('DATA_ARCHIVE_EXEC_ATTR_TAB', TRUE);
   Installation_SYS.Remove_Table('DATA_ARCHIVE_LOG_TAB', TRUE);
   Installation_SYS.Remove_Table('DATA_ARCHIVE_OBJECT_TAB', TRUE);
   Installation_SYS.Remove_Table('DATA_ARCHIVE_ORDER_EXEC_TAB', TRUE);
   Installation_SYS.Remove_Table('DATA_ARCHIVE_ORDER_TAB', TRUE);
   Installation_SYS.Remove_Table('DATA_ARCHIVE_SOURCE_ATTR_TAB', TRUE);
   Installation_SYS.Remove_Table('DATA_ARCHIVE_SOURCE_TAB', TRUE);
   Installation_SYS.Remove_Table('DB_SCRIPT_REGISTER_DETAIL_TAB', TRUE);
   Installation_SYS.Remove_Table('DB_SCRIPT_REGISTER_TAB', TRUE);
   Installation_SYS.Remove_Table('DICTIONARY_SYS_DOMAIN_TAB', TRUE);
   Installation_SYS.Remove_Table('DICTIONARY_SYS_STATE_EVENT_TAB', TRUE);
   Installation_SYS.Remove_Table('DICTIONARY_SYS_STATE_MACH_TAB', TRUE);
   Installation_SYS.Remove_Table('DICTIONARY_SYS_STATE_TAB', TRUE);
   Installation_SYS.Remove_Table('DICTIONARY_SYS_STATE_TRANS_TAB', TRUE);
   Installation_SYS.Remove_Table('DICTIONARY_SYS_VIEW_TAB', TRUE);
   Installation_SYS.Remove_Table('DISTRIBUTION_GROUP_MEMBER_TAB', TRUE);
   Installation_SYS.Remove_Table('DISTRIBUTION_GROUP_TAB', TRUE);
   Installation_SYS.Remove_Table('DRAFT_TAB', TRUE);
   Installation_SYS.Remove_Table('DSOD_CRITERIA_TAB', TRUE);
   Installation_SYS.Remove_Table('DSOD_DISPLAY_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('DSOD_GROUPING_TAB', TRUE);
   Installation_SYS.Remove_Table('DSOD_SET_TAB', TRUE);
   Installation_SYS.Remove_Table('DSOD_SORTING_TAB', TRUE);
   Installation_SYS.Remove_Table('DSOD_STRUCTURE_REQUEST_TAB', TRUE);
   Installation_SYS.Remove_Table('DSOD_TAB', TRUE);
   Installation_SYS.Remove_Table('EXCEL_REPORT_ARCHIVE_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_CODE_TEMPLATE_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_EVENT_ACTION_SUBSCRIBE_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_EVENT_MY_MESSAGES_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_EVENT_PARAMETER_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_GRANT_ROLE_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_GRANT_ROLE_TMP', TRUE);
   Installation_SYS.Remove_Table('FND_LICENSE_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_MONITOR_CATEGORY_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_MONITOR_ENTRY_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_NOTE_BOOK_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_NOTE_PAGE_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_SETTING_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_USER_ROLE_RUNTIME_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_USER_ROLE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_ADDRESS_LABEL_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_APPLICATION_MESSAGE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_CACHED_ARTIFACT_ELEM_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_CACHED_ARTIFACT_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_CONDITION_PART_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_CONFIG_PARAM_AREA_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_CONFIG_PARAM_GROUP_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_CONFIG_PARAM_INST_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_CONFIG_PARAM_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_IFACE_CONTROL_PARAM_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_LOGICAL_UNIT_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_LUATTRIBUTE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_LUOPERATION_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_MESSAGE_BODY_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_MOD_SYS_DOCUMENT_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_MSG_ARCHIVE_ADDR_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_MSG_ARCHIVE_BODY_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_MSG_ARCHIVE_SEARCH_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_MSG_ARCHIVE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_RECURRENCE_AGENDA_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_RECURRENCE_PATTERN_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_ROUTE_ADDRESS_REF_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_ROUTE_ADDRESS_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_ROUTE_CONDITION_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_TARGET_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDCN_UML_DIAGRAM_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_ACTIVITY_ENT_FILTR_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_ACTIVITY_PKG_DIAG_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_ASSOC_STATE_VALID_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_ATTR_STAT_VALIDATION_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_ATTR_VALIDATION_RULE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_CLIENT_DIAGRAM_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_CLIENTPLUGIN_ACT_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_CLIENTPLUGIN_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_DETAIL_VALIDATION_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_ENTITY_DIAGRAM_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_ENTITY_PACKAGE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_ENTITY_PKG_DIAGRAM_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_FEATURE_WIDGET_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_FILTER_PARAMETER_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_HANDLER_METHOD_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_HANDLER_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_INDEX_COLUMN_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_J2EE_APPLICATION_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_J2EE_MODULE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_LOGICAL_UNIT_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_MAPPED_ENTITY_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_METHOD_FILTER_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_METHOD_PARAMETER_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_MODEL_IMPORT_LOG_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_MODEL_WORKSPACE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_MODULE_DIAGRAM_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_PACKAGE_DEPENDENCY_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_PACKAGE_DIAGRAM_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_PARAMETER_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_PLSQL_METHOD_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_PLSQL_PACKAGE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_PLSQL_PARAMETER_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_PROJECT_SETTING_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_REPORT_PARAMETER_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_REPORT_TEXT_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_SERV_PKG_DEPENDENCY_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_STATE_DIAGRAM_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_STATE_EVENT_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_STORAGE_ATTRIBUTE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_STORAGE_DIAGRAM_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_STORAGE_PACKAGE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_TABLE_INDEX_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_VALIDATION_OBJECT_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_WIDGET_ACTIVITY_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDDR_WIDGET_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDMOB_ENTITY_SYNCH_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDMOB_OPTIMIZER_DATA_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDMOB_USER_CACHE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDRR_USER_CLIENT_PROFILE_TAB', TRUE);
   Installation_SYS.Remove_Table('FUNC_AREA_CONFLICT_CACHE_TAB', TRUE);
   Installation_SYS.Remove_Table('FUNC_AREA_SEC_CACHE_TAB', TRUE);
   Installation_SYS.Remove_Table('FUNCTIONAL_AREA_ACTIVITY_TAB', TRUE);
   Installation_SYS.Remove_Table('FUNCTIONAL_AREA_CONFLICT_TAB', TRUE);
   Installation_SYS.Remove_Table('FUNCTIONAL_AREA_METHOD_TAB', TRUE);
   Installation_SYS.Remove_Table('FUNCTIONAL_AREA_VIEW_TAB', TRUE);
   Installation_SYS.Remove_Table('HISTORY_LOG_ATTRIBUTE_TAB', TRUE);
   Installation_SYS.Remove_Table('HISTORY_SETTING_ATTRIBUTE_TAB', TRUE);
   Installation_SYS.Remove_Table('HISTORY_SETTING_TAB', TRUE);
   Installation_SYS.Remove_Table('IAL_OBJECT_TAB', TRUE);
   Installation_SYS.Remove_Table('IN_MESSAGE_LOADED_TAB', TRUE);
   Installation_SYS.Remove_Table('INSTALL_TEM_SYS_TAB', TRUE);
   Installation_SYS.Remove_Table('INSTALLATION_SITE_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_CONNECTION_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_CONTENT_TYPE_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_DESTINATION_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_FILE_EXPORT_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_FILE_IMPORT_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_FONT_MAPPING_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_PROPERTY_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_SOURCE_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_SYS_IMP_TAB', TRUE);
   Installation_SYS.Remove_Table('LDAP_CONFIGURATION_TAB', TRUE);
   Installation_SYS.Remove_Table('LDAP_DOMAIN_CONFIG_TAB', TRUE);
   Installation_SYS.Remove_Table('LDAP_MAPPING_TAB', TRUE);
   Installation_SYS.Remove_Table('LOGICAL_PRINTER_TAB', TRUE);
   Installation_SYS.Remove_Table('MESSAGE_CLASS_TAB', TRUE);
   Installation_SYS.Remove_Table('MESSAGE_MEDIA_TAB', TRUE);
   Installation_SYS.Remove_Table('MESSAGE_RECEIVER_TAB', TRUE);
   Installation_SYS.Remove_Table('MODULE_DB_PATCH_TAB', TRUE);
   Installation_SYS.Remove_Table('MODULE_DEPENDENCY_TAB', TRUE);
   Installation_SYS.Remove_Table('MY_TODO_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('NOTE_BOOK_TAB', TRUE);
   Installation_SYS.Remove_Table('NOTE_TAB', TRUE);
   Installation_SYS.Remove_Table('OUT_MESSAGE_LINE_TAB', TRUE);
   Installation_SYS.Remove_Table('OUT_MESSAGE_TAB', TRUE);
   Installation_SYS.Remove_Table('PDF_ARCHIVE_TAB', TRUE);
   Installation_SYS.Remove_Table('PERFORMANCE_ANALYZE_SOURCE_TAB', TRUE);
   Installation_SYS.Remove_Table('PERMISSION_SET_FILTER_PAR_TAB', TRUE);
   Installation_SYS.Remove_Table('PERMISSION_SET_FILTER_TAB', TRUE);
   Installation_SYS.Remove_Table('PLSQLAP_BUFFER_TMP', TRUE);
   Installation_SYS.Remove_Table('PLSQLAP_ENVIRONMENT_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_BUILD_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_CHANGE_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_DEP_BUILD_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_DEP_CHANGE_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_DEPENDENCY_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_DESCRIPTION_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_EXCLUDE_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_INCLUDE_SEC_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_INCLUDE_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_SEC_CHANGE_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_SEC_EXPORT_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_SECURITY_BUILD_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_SECURITY_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_SECURITY_TYPE_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_TYPE_TAB', TRUE);
   Installation_SYS.Remove_Table('PRINT_JOB_TAB', TRUE);
   Installation_SYS.Remove_Table('PRINT_QUEUE_TAB', TRUE);
   Installation_SYS.Remove_Table('QUERY_HINT_COL_TAB', TRUE);
   Installation_SYS.Remove_Table('QUERY_HINT_INDEX_TAB', TRUE);
   Installation_SYS.Remove_Table('QUERY_HINT_TABLE_TAB', TRUE);
   Installation_SYS.Remove_Table('QUERY_HINT_VIEW_TAB', TRUE);
   Installation_SYS.Remove_Table('QUICK_REPORT_TAB', TRUE);
   Installation_SYS.Remove_Table('RECEIVE_ATTR_GROUP_TAB', TRUE);
   Installation_SYS.Remove_Table('RECEIVE_ATTR_TAB', TRUE);
   Installation_SYS.Remove_Table('RECEIVE_CONDITION_TAB', TRUE);
   Installation_SYS.Remove_Table('RECEIVE_GROUP_TAB', TRUE);
   Installation_SYS.Remove_Table('RECEIVE_OBJECT_TAB', TRUE);
   Installation_SYS.Remove_Table('REFERENCE_SYS_TAB', TRUE);
   Installation_SYS.Remove_Table('REMOTE_PRINTER_MAPPING_TAB', TRUE);
   Installation_SYS.Remove_Table('REMOTE_PRINTING_NODE_TAB', TRUE);
   Installation_SYS.Remove_Table('REPLICATION_ATTR_DEF_TAB', TRUE);
   Installation_SYS.Remove_Table('REPLICATION_ATTR_GROUP_DEF_TAB', TRUE);
   Installation_SYS.Remove_Table('REPLICATION_ATTR_GROUP_TAB', TRUE);
   Installation_SYS.Remove_Table('REPLICATION_CONDITION_TAB', TRUE);
   Installation_SYS.Remove_Table('REPLICATION_GROUP_TAB', TRUE);
   Installation_SYS.Remove_Table('REPLICATION_LOG_TAB', TRUE);
   Installation_SYS.Remove_Table('REPLICATION_OBJECT_DEF_TAB', TRUE);
   Installation_SYS.Remove_Table('REPLICATION_OBJECT_TAB', TRUE);
   Installation_SYS.Remove_Table('REPLICATION_QUEUE_TAB', TRUE);
   Installation_SYS.Remove_Table('REPLICATION_RECEIVER_TAB', TRUE);
   Installation_SYS.Remove_Table('REPLICATION_SENDER_TAB', TRUE);
   Installation_SYS.Remove_Table('REPLICATION_STATISTICS_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_CATEGORY_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_FONT_DEFINITION_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_IN_PROGRESS_TMP', TRUE);
   Installation_SYS.Remove_Table('REPORT_LAYOUT_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_LAYOUT_TYPE_CONFIG_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_LU_DEFINITION_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_PDF_INSERT_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_PLUGIN_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_RESULT_GEN_CONFIG_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_RULE_ACTION_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_RULE_CONDITION_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_RULE_DEFINITION_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_RULE_LOG_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_SCHEMA_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_SYS_COLUMN_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_SYS_GROUP_COLUMN_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_SYS_GROUP_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_SYS_TEXT_TAB', TRUE);
   Installation_SYS.Remove_Table('REPORT_USER_SETTINGS_TAB', TRUE);
   Installation_SYS.Remove_Table('SEARCH_DOMAIN_GROUP_MEMBER_TAB', TRUE);
   Installation_SYS.Remove_Table('SEARCH_DOMAIN_GROUP_TAB', TRUE);
   Installation_SYS.Remove_Table('SEARCH_DOMAIN_UNSYNCHED_TAB', TRUE);
   Installation_SYS.Remove_Table('SEC_CHECKPOINT_GATE_PARAM_TAB', TRUE);
   Installation_SYS.Remove_Table('SEC_CHECKPOINT_GATE_TAB', TRUE);
   Installation_SYS.Remove_Table('SEC_CHECKPOINT_LOG_TAB', TRUE);
   Installation_SYS.Remove_Table('SECURITY_SYS_EXPANDED_ROLE_TAB', TRUE);
   Installation_SYS.Remove_Table('SECURITY_SYS_PRIVS_TAB', TRUE);
   Installation_SYS.Remove_Table('SECURITY_SYS_REFRESH_USER_TAB', TRUE);
   Installation_SYS.Remove_Table('SECURITY_SYS_ROLE_TREE_TAB', TRUE);
   Installation_SYS.Remove_Table('SECURITY_SYS_TAB', TRUE);
   Installation_SYS.Remove_Table('SELECTION_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('SELECTION_TAB', TRUE);
   Installation_SYS.Remove_Table('SERVER_LOG_CATEGORY_TAB', TRUE);
   Installation_SYS.Remove_Table('SERVER_LOG_TAB', TRUE);
   Installation_SYS.Remove_Table('SITE_TEXT_TAB', TRUE);
   Installation_SYS.Remove_Table('SOD_CACHE_TAB', TRUE);
   Installation_SYS.Remove_Table('SOX_FUNCTIONAL_AREA_TAB', TRUE);
   Installation_SYS.Remove_Table('SYSTEM_PRIVILEGE_GRANT_TAB', TRUE);
   Installation_SYS.Remove_Table('SYSTEM_PRIVILEGE_TAB', TRUE);
   Installation_SYS.Remove_Table('TEMPLATE_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_DEFINITION_HISTORY_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_HISTORY_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_RELATION_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_STOP_LIST_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_USAGE_IDENTIFIER_TAB', TRUE);
   Installation_SYS.Remove_Table('TODO_FOLDER_TAB', TRUE);
   Installation_SYS.Remove_Table('TODO_HISTORY_TAB', TRUE);
   Installation_SYS.Remove_Table('TODO_ITEM_OWNER', TRUE);
   Installation_SYS.Remove_Table('TODO_ITEM_RECEIVER_TAB', TRUE);
   Installation_SYS.Remove_Table('TODO_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('TRANSACTION_SYS_STATUS_TAB', TRUE);
   Installation_SYS.Remove_Table('USER_PROFILE_SYS_TAB', TRUE);
   Installation_SYS.Remove_Table('WORKSPACE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_BA_CLIENT_ARCHIVE_DIST_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_BA_CLIENT_ARCHIVE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_DEF_FACT_NAV_FOLDER_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_DIM_ITEM_STRUCT_INFO_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_DIM_SOURCE_HINT_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_DIM_SOURCE_IX_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_DIM_SOURCE_IX_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_DIMENSION_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_DIMENSION_LOV_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_DIMENSION_LOV_SORT_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_DIMENSION_PARENT_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_DIMENSION_SOURCE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_DETAIL_DIM_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_DIM_ID_DEF_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_DIM_JOIN_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_DIM_JOIN_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_DIM_SRC_INDICATE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_DIMENSION_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_PARENT_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_SOURCE_HINT_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_SOURCE_IX_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_SOURCE_IX_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_SOURCE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_URL_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_URL_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_WB_DIM_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_WRITE_BACK_INFO_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_IMPORT_LOG_GLOB_ID_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_IMPORT_LOG_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_INSTANCE_PARAM_VALUE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_LOG_LARGE_STORE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_LOG_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_META_DATA_FILE_ENTITY_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_MV_CRITERIA_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_MV_HELP_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_MV_INFO_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_MV_LIST_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_MV_PER_REFRESH_CAT_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_MV_REF_ALL_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_MV_REF_HELP_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_MV_REF_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_MV_REF_TEMP_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_MV_REFRESH_CATEGORY_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_REPORT_INSTANCE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_REPORT_RENDERING_INFO_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_REPORT_RESULT_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_REPORT_UPG_UTILITY_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_SESSION_PARAM_VALUE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_SOURCE_NAVIGATOR_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_STRUCT_REQUEST_PARENT_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_STRUCT_REQUEST_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_STRUCT_RESPONSE_LEAF_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_STRUCT_RESPONSE_LEVEL_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_STRUCT_RESPONSE_NODE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_STRUCT_RESPONSE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_SYSTEM_PARAMETER_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_TEMPLATE_FILE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_TEMPLATE_PARAMETER_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_TEMPLATE_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_USER_DEP_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WB_COLLECTION_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WB_CONTENT_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WB_FACT_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WB_ITEM_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WB_ROW_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WB_SET_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WRITE_BACK_COLLECTION_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WRITE_BACK_ITEM_DETAIL_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WRITE_BACK_MASTER_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WRITE_BACK_ROW_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WRITE_BACK_SET_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WRITE_BACK_SET_UQ_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_WRITE_BACK_VALUE_TAB', TRUE);

END;
/

----------------------------------------------------------------
-- NOTE: Tables below are belongs to the current module but are also
--       referred by other objects in other modules
--       These tables should be dropped BUT all the references will be
--       Invalidated
----------------------------------------------------------------

BEGIN
   dbms_output.put_line('Removing Tables for the current module but also referred by other Modules as well...');
   Installation_SYS.Remove_Table('ARCHIVE_TAB', TRUE);
   Installation_SYS.Remove_Table('BATCH_SCHEDULE_METHOD_TAB', TRUE);
   Installation_SYS.Remove_Table('BATCH_SCHEDULE_PAR_TAB', TRUE);
   Installation_SYS.Remove_Table('BATCH_SCHEDULE_TAB', TRUE);
   Installation_SYS.Remove_Table('BI_TIME_DIMENSION_TAB', TRUE);
   Installation_SYS.Remove_Table('DOCUMENT_LIFECYCLE_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_EVENT_ACTION_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_EVENT_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_ROLE_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_SESSION_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_TRANSLATION_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_USER_PROPERTY_TAB', TRUE);
   Installation_SYS.Remove_Table('FND_USER_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDRR_CLIENT_PROFILE_TAB', TRUE);
   Installation_SYS.Remove_Table('FNDRR_CLIENT_PROFILE_VALUE_TAB', TRUE);
   Installation_SYS.Remove_Table('HISTORY_LOG_TAB', TRUE);
   Installation_SYS.Remove_Table('IN_MESSAGE_LINE_TAB', TRUE);
   Installation_SYS.Remove_Table('IN_MESSAGE_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_ATTRIBUTE_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_CODE_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_CONTEXT_TAB', TRUE);
   Installation_SYS.Remove_Table('LANGUAGE_TRANSLATION_TAB', TRUE);
   Installation_SYS.Remove_Table('MODULE_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_GRANT_TAB', TRUE);
   Installation_SYS.Remove_Table('PRES_OBJECT_TAB', TRUE);
   Installation_SYS.Remove_Table('PRINT_JOB_CONTENTS_TAB', TRUE);
   Installation_SYS.Remove_Table('SEARCH_DOMAIN_DOCUMENT_TAB', TRUE);
   Installation_SYS.Remove_Table('SEARCH_DOMAIN_RUNTIME_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_ALERT_INDICATOR_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_DISPLAY_NAME_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_DOMAIN_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_OWNER_MODULE_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_OWNER_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_TRANSLATED_DEFINITION_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_TRANSLATED_NAME_TAB', TRUE);
   Installation_SYS.Remove_Table('TERM_USAGE_DEFINITION_TAB', TRUE);
   Installation_SYS.Remove_Table('USER_GROUP_TAB', TRUE);
   Installation_SYS.Remove_Table('USER_GROUP_USER_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_DIM_REF_RELATIONS_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_DIMENSION_TAB', TRUE);
   Installation_SYS.Remove_Table('XLR_FACT_TAB', TRUE);
   Installation_SYS.Remove_Table('XML_REPORT_DATA_TAB', TRUE);
END;
/


----------------------------------------------------------------
-- Removing patch registrations for this module
----------------------------------------------------------------
BEGIN
   dbms_output.put_line('Removing Patch Registrations for the Module...');
   Installation_SYS.Clear_Db_Patch_Registration('FNDBAS');
END;
/

----------------------------------------------------------------
-- Removing module version information
----------------------------------------------------------------
BEGIN
   dbms_output.put_line('Removing Module Version Information for the Module...');
   Module_Api.Clear('FNDBAS');
END;
/




COMMIT;


----------------------------------------------------------------
--Re Compiling All Invalid Objects
----------------------------------------------------------------
PROMPT Re Compiling All Invalid Objects
EXEC Database_SYS.Compile_All_Invalid_Objects;

----------------------------------------------------------------
-- End
----------------------------------------------------------------


SPOOL OFF


SET SERVEROUTPUT OFF

