package org.ssyang.usefulservice;

import com.sun.grizzly.util.buf.Base64;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.sun.jersey.multipart.FormDataMultiPart;
import com.sun.jersey.multipart.file.FileDataBodyPart;
import com.sun.jersey.multipart.impl.MultiPartWriter;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// The Java class will be hosted at the URI path "/myresource"
@Path("/http")
public class HttpService {
	private String regex = "<FileID>(.*)</FileID>";
	private Pattern p = Pattern.compile(regex);

	private String regex2 = "<Result>(.*)</Result>";
	private Pattern p2 = Pattern.compile(regex2);


	@POST
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/uploadToOcr")
	public String uploadToOcr(@FormParam("base64Data") String base64Data) throws IOException {
		byte[] data = new Base64().decode(base64Data.replaceAll("=", "").getBytes());
		File file = new File("/tmp/pic.png");
		if (!file.exists()) {
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			file.createNewFile();
		}
		FileOutputStream fileOutputStream = new FileOutputStream(file, false);

		fileOutputStream.write(data);

		fileOutputStream.flush();

		fileOutputStream.close();

		FormDataMultiPart part = new FormDataMultiPart();
		part.bodyPart(new FileDataBodyPart("Filename", file));

		ClientConfig cc = new DefaultClientConfig();
		cc.getClasses().add(MultiPartWriter.class);
		Client writerClient = Client.create(cc);
		// 处理文件将超时设置为10S
		writerClient.setConnectTimeout(3000);
		writerClient.setReadTimeout(3000);
		try {
			WebResource resource = writerClient.resource("http://lab.ocrking.com/upload.html");
			String response = resource.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(String.class, part);
			Matcher m = p.matcher(response);
			if (m.find()) {

				Client client = new Client();
				try {
					String fid = m.group(1);
					WebResource webResource = client.resource("http://lab.ocrking.com/do.html");
					MultivaluedMap<String, String> param = new MultivaluedMapImpl();

					param.add("service", "OcrKingForNumber");
					param.add("language", "eng");
					param.add("charset", "1");
					param.add("outputFormat", "");
					param.add("upfile", "true");
					param.add("fileID", fid);

					String res = webResource.entity(param).type(MediaType.APPLICATION_FORM_URLENCODED).post(String.class);
					Matcher m2 = p2.matcher(res);
					if (m2.find()) {
						return m2.group(1);
					}
				} finally {
					client.destroy();
				}
			}
			return null;
		} finally {
			writerClient.destroy();
		}


	}
}
