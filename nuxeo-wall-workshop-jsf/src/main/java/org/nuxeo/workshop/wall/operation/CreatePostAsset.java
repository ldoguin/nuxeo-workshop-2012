/**
 * 
 */

package org.nuxeo.workshop.wall.operation;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.audio.extension.AudioImporter;
import org.nuxeo.ecm.platform.filemanager.service.extension.DefaultFileImporter;
import org.nuxeo.ecm.platform.picture.api.ImagingDocumentConstants;
import org.nuxeo.ecm.platform.video.VideoConstants;
import org.nuxeo.externalresource.ExternalResourceConstants;
import org.nuxeo.externalresource.provider.ExternalResourceProvider;
import org.nuxeo.externalresource.provider.ExternalResourceProviderService;

/**
 * @author ldoguin
 */
@Operation(id = CreatePostAsset.ID, category = Constants.CAT_DOCUMENT, label = "CreatePostAsset", description = "")
public class CreatePostAsset {

	public static final String ID = "CreatePostAsset";

	protected Pattern URL_PATTERN = Pattern
			.compile("(.*)((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])(.*)",
					Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	@Context
	protected OperationContext operationContext;

	@Context
	protected CoreSession coreSession;

	@Context
	protected ExternalResourceProviderService externalResourceService;

	protected List<ExternalResourceProvider> providerInstanceList;

	@OperationMethod(collector = DocumentModelCollector.class)
	public DocumentModel run(DocumentModel input) throws ClientException,
			IOException {
		Blob asset = (Blob) operationContext.get("uploadedAsset");
		if (asset == null || asset.getLength() == 0l) {
			return searchForLink(input);
		}
		String blobMimeType = asset.getMimeType();
		if (blobMimeType.startsWith("video")) {
			return createAsset(input, VideoConstants.VIDEO_TYPE, asset);
		} else if (blobMimeType.startsWith("audio")) {
			return createAsset(input, AudioImporter.AUDIO_TYPE, asset);
		} else if (blobMimeType.startsWith("image")) {
			return createAsset(input,
			 	ImagingDocumentConstants.PICTURE_TYPE_NAME, asset);
		} else {
			return createAsset(input, DefaultFileImporter.TYPE_NAME, asset);
		}
	}

	protected DocumentModel createAsset(DocumentModel parent, String type,
			Blob asset) throws ClientException {
		DocumentModel postAsset = coreSession.createDocumentModel(
				parent.getPathAsString(), asset.getFilename(), type);
		postAsset.setPropertyValue("dc:title",
				(Serializable) asset.getFilename());
		postAsset.setPropertyValue("file:content", (Serializable) asset);
		postAsset = coreSession.createDocument(postAsset);
		return postAsset;
	}

	protected DocumentModel searchForLink(DocumentModel input)
			throws PropertyException, ClientException {
		String text = input.getProperty("dc:description")
				.getValue(String.class);
		Matcher m = URL_PATTERN.matcher(text);
		if (!m.matches()) {
			return null;
		} else {
			String link = m.group(2);
			for (ExternalResourceProvider provider : getProviderOptions()) {
				if (provider.match(link)) {
					Map<String, Serializable> properties = provider
							.getProperties(link);
					if (properties != null) {
						String html = (String) properties
								.get(ExternalResourceConstants.EXTERNAL_RESOURCE_HTML_KEY);
						String title = (String) properties
								.get(ExternalResourceConstants.EXTERNAL_RESOURCE_TITLE_KEY);
						DocumentModel asset = coreSession.createDocumentModel(
								input.getPathAsString(), title,
								"ExternalResource");
						asset.setPropertyValue("dc:title", title);
						asset.setPropertyValue(
								ExternalResourceConstants.EXTERNAL_RESOURCE_HTML_PROPERTY_NAME,
								html);
						asset.setPropertyValue(
								ExternalResourceConstants.EXTERNAL_RESOURCE_LINK_PROPERTY_NAME,
								text);
						asset.setPropertyValue(
								ExternalResourceConstants.EXTERNAL_RESOURCE_PROVIDER_ICON_PROPERTY_NAME,
								provider.getIcon());
						asset.setPropertyValue(
								ExternalResourceConstants.EXTERNAL_RESOURCE_PROVIDER_PROPERTY_NAME,
								provider.getName());
						asset = coreSession.createDocument(asset);
						return asset;
					}
				}
			}
		}
		return null;
	}

	protected boolean match(String url) {
		Matcher m = URL_PATTERN.matcher(url);
		if (m.matches()) {

			return true;
		}
		return false;
	}

	protected List<ExternalResourceProvider> getProviderOptions() {
		if (providerInstanceList == null) {
			providerInstanceList = externalResourceService.getProviders();
		}
		return providerInstanceList;
	}

}
