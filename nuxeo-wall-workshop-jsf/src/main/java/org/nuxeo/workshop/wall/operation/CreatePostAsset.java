/**
 * 
 */

package org.nuxeo.workshop.wall.operation;

import java.io.IOException;
import java.io.Serializable;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.audio.extension.AudioImporter;
import org.nuxeo.ecm.platform.filemanager.service.extension.DefaultFileImporter;
import org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants;
import org.nuxeo.ecm.platform.picture.api.adapters.PictureResourceAdapter;
import org.nuxeo.ecm.platform.video.VideoConstants;

/**
 * @author ldoguin
 */
@Operation(id=CreatePostAsset.ID, category=Constants.CAT_DOCUMENT, label="CreatePostAsset", description="")
public class CreatePostAsset {

    public static final String ID = "CreatePostAsset";
    
    @Context
    protected OperationContext operationContext;

    @Context
    protected CoreSession coreSession;

    @OperationMethod(collector=DocumentModelCollector.class)
    public DocumentModel run(DocumentModel input) throws ClientException, IOException {
      Blob asset = (Blob) operationContext.get("uploadedAsset");
      if (asset == null) {
    	  return null;
      }
      String blobMimeType = asset.getMimeType();
      if (blobMimeType.startsWith("video")) {
          return createAsset(input, VideoConstants.VIDEO_TYPE, asset);
      } else if (blobMimeType.startsWith("audio")) {
          return createAsset(input, AudioImporter.AUDIO_TYPE, asset);
      } else if (blobMimeType.startsWith("image")) {
          return createAsset(input, ImagingDocumentConstants.PICTURE_TYPE_NAME, asset);
      } else {
          return createAsset(input, DefaultFileImporter.TYPE_NAME, asset);
      }
    }    

    private DocumentModel createAsset(DocumentModel parent, String type, Blob asset) throws ClientException {
        DocumentModel postAsset = coreSession.createDocumentModel(parent.getPathAsString(), asset.getFilename(), type);
        postAsset.setPropertyValue("dc:title", (Serializable) asset.getFilename());
        postAsset.setPropertyValue("file:content", (Serializable) asset);
        postAsset = coreSession.createDocument(postAsset);
        return postAsset;
	}
}
