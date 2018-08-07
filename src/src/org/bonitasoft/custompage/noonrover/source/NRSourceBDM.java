package org.bonitasoft.custompage.noonrover.source;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.bind.JAXBException;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusAttribute;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition.TYPECOLUMN;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition.TYPESOURCE;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinitionFactory;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSelection;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSelection.TYPESELECTION;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.api.TenantAdministrationAPI;
import org.bonitasoft.engine.bdm.BusinessObjectModelConverter;
import org.bonitasoft.engine.bdm.model.BusinessObject;
import org.bonitasoft.engine.bdm.model.BusinessObjectModel;
import org.bonitasoft.engine.bdm.model.Query;
import org.bonitasoft.engine.bdm.model.QueryParameter;
import org.bonitasoft.engine.bdm.model.field.Field;
import org.bonitasoft.engine.bdm.model.field.FieldType;
import org.bonitasoft.engine.bdm.model.field.RelationField;
import org.bonitasoft.engine.bdm.model.field.SimpleField;
import org.bonitasoft.engine.business.data.BusinessDataRepositoryException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.xml.sax.SAXException;

public class NRSourceBDM extends NRSource {

    public static BEvent EventErrorTenantAdministration = new BEvent(NRSourceBDM.class.getName(), 1, Level.ERROR,
            "Access the Tenant Administration", "To get the BDM, the TenantAdministration has to be access",
            "No BDM object can be accessed",
            "Fix the error");
    public static BEvent EventNoBdm = new BEvent(NRSourceBDM.class.getName(), 2, Level.APPLICATIONERROR,
            "No Business Data Object", "This tenant does not have any Business Data Object deployed",
            "No BDM object can be accessed",
            "Import a BDM");
    public static BEvent EventBdmError = new BEvent(NRSourceBDM.class.getName(), 3, Level.ERROR,
            "Business Data Object can't be decoded",
            "The BDM information are correctly retrieved, but it's decodage failed", "No BDM object can be accessed",
            "Fix the error");

    public SourceStatus getListBusinessDefinition(APISession apiSession, NRBusDefinitionFactory businessFactory) {
        SourceStatus sourceStatus = new SourceStatus();

        try {
            TenantAdministrationAPI tenantAdmininstrationAPI = TenantAPIAccessor.getTenantAdministrationAPI(apiSession);
            BusinessObjectModelConverter converter = new BusinessObjectModelConverter();

            byte[] bdmZip = tenantAdmininstrationAPI.getClientBDMZip();

            //  FileOutputStream fileBdm = new FileOutputStream("c:/temp/bdm.zip");
            //  fileBdm.write(bdmZip);
            //  fileBdm.close();

            byte[] bomZip = getBomZip(bdmZip);

            BusinessObjectModel bom = converter.unzip(bomZip);

            for (BusinessObject businessObject : bom.getBusinessObjects()) {
                NRBusDefinition businessDefinition = businessFactory.createDataDefinitionBDM(businessObject );
                businessDefinition.setTableName(businessObject.getSimpleName());
                businessDefinition.setTypeSource(TYPESOURCE.BDM);
                businessDefinition.setDescription(businessObject.getDescription());

              
                // add a sTANDARD Sql request
                NRBusSelection standard = businessDefinition.getInstanceBusSelection("Standard");
                standard.typeFind = TYPESELECTION.STD;

                // first field : the PERSISTENCEID
                NRBusAttribute attributePersistenceId = businessDefinition.getInstanceAttribute(businessObject.getSimpleName(), "PersistenceId", TYPECOLUMN.NUM,false);
                businessDefinition.result.addListResultsetColumFromAttribute(attributePersistenceId);
                standard.getInstanceSelectionParameter(attributePersistenceId.name, attributePersistenceId.type);
              
                
                // collect list of result
                for (Field field : businessObject.getFields()) {
                    TYPECOLUMN typeField;
                    NRBusAttribute attributeDefinition =null;
                    if (field instanceof SimpleField) {
                        typeField = getTypeFromFieldType(((SimpleField) field).getType());
                        attributeDefinition = businessDefinition.getInstanceAttribute(businessObject.getSimpleName(), field.getName(), typeField, field.isCollection());
                        attributeDefinition.length = ((SimpleField) field).getLength();
                    } else if (field instanceof RelationField)
                    {
                        typeField= TYPECOLUMN.RELATION;
                        attributeDefinition = businessDefinition.getInstanceRelationAttribute(businessObject.getSimpleName(), field.getName(), ((RelationField)field).getReference().getSimpleName(), field.isCollection());
                        
                    } else
                    {
                        typeField = TYPECOLUMN.STRING;
                        attributeDefinition = businessDefinition.getInstanceAttribute(businessObject.getSimpleName(), field.getName(), typeField, field.isCollection());
                    }


                 
                    businessDefinition.result.addListResultsetColumFromAttribute(attributeDefinition);
                    standard.getInstanceSelectionParameter(attributeDefinition.name, attributeDefinition.type);
                    // then one find per standard column
                    if (field instanceof SimpleField) {
                        NRBusSelection findByField = businessDefinition
                                .getInstanceBusSelection("findBy" + field.getName());
                        findByField.typeFind = TYPESELECTION.FIND;
                        findByField.setObjectTransported(field);
                        findByField.getInstanceSelectionParameter(field.getName(),
                                getTypeFromFieldType(((SimpleField) field).getType()));
                    }
                }
                // the selection : one per find
                for (Query query : businessObject.getQueries()) {
                    // create 2 more find : the DirectSQL and the Standard one
                    NRBusSelection findBy = businessDefinition.getInstanceBusSelection(query.getName());
                    findBy.typeFind = TYPESELECTION.FIND;
                    findBy.setObjectTransported(query);;
                    for (QueryParameter queryParameters : query.getQueryParameters()) {
                        findBy.getInstanceSelectionParameter(queryParameters.getName(),
                                getTypeFromQueryClassName(queryParameters.getClassName()));
                    }
                }

                // add a DirectSql request
                NRBusSelection directSQL = businessDefinition.getInstanceBusSelection("Direct SQL");
                directSQL.typeFind = TYPESELECTION.SQL;

                sourceStatus.listBusinessDefinition.add(businessDefinition);

                // one selection per query

            }

        } catch (ServerAPIException | UnknownAPITypeException | BonitaHomeNotSetException e) {
            sourceStatus.listEvents.add(new BEvent(EventErrorTenantAdministration, e, ""));
        } catch (BusinessDataRepositoryException e) {
            sourceStatus.listEvents.add(new BEvent(EventNoBdm, e, ""));

        } catch (IOException e) {
            sourceStatus.listEvents.add(new BEvent(EventErrorTenantAdministration, e, ""));
        } catch (JAXBException | SAXException e) {
            sourceStatus.listEvents.add(new BEvent(EventBdmError, e, ""));
        } catch (Exception e) {
            sourceStatus.listEvents.add(new BEvent(EventBdmError, e, ""));
        }
        return sourceStatus;

    }

    private TYPECOLUMN getTypeFromQueryClassName(String className) {
        if (className.equals("java.lang.Integer") || className.equals("java.lang.Long"))
            return TYPECOLUMN.NUM;
        if (className.equals("java.time.LocalDate")
                || className.equals("java.time.LocalDateTime")
                || className.equals("java.time.OffsetDateTime")
                || className.equals("java.util.Date"))
            return TYPECOLUMN.DATE;
        // this is a TEXT
        if (className == "java.lang.Boolean")
            return TYPECOLUMN.BOOLEAN;

        return TYPECOLUMN.STRING;
    }

    public TYPECOLUMN getTypeFromFieldType(FieldType fieldType) {
        if (fieldType == fieldType.CHAR)
            return TYPECOLUMN.STRING;
        if ((fieldType == fieldType.DOUBLE) || (fieldType == fieldType.INTEGER) || (fieldType == fieldType.FLOAT)
                || (fieldType == fieldType.LONG))
            return TYPECOLUMN.NUM;
        if (fieldType == fieldType.BOOLEAN)
            return TYPECOLUMN.BOOLEAN;
        if (fieldType == fieldType.DATE || (fieldType == fieldType.LOCALDATE) || (fieldType == fieldType.LOCALDATETIME)
                || (fieldType == fieldType.OFFSETDATETIME))
            return TYPECOLUMN.DATE;
        return TYPECOLUMN.STRING;

    }

    /**
     * Unzip the BDM zip to get the correct bom.zip
     * 
     * @param bdmZip
     * @return
     * @throws Exception
     */
    private byte[] getBomZip(byte[] bdmZip) throws Exception {
        //get the zip file content
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bdmZip));
        //get the zipped file list entry
        ZipEntry ze = zis.getNextEntry();
        byte[] buffer = new byte[1024];

        while (ze != null) {

            String fileName = ze.getName();
            if (fileName.equals("bom.zip")) {
                ByteArrayOutputStream fos = new ByteArrayOutputStream();

                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }

                fos.close();
                return fos.toByteArray();
            }
            ze = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
        throw new Exception("Bad bdm.zip structure: file[bom.zip] is not present");
    }
}
