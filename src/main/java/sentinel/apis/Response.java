package sentinel.apis;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;

import sentinel.utils.StringUtils;

public class Response {
	
	protected HttpResponse response;
	protected String jsonResponse;
	
	public Response(HttpResponse httpResponse) throws UnsupportedOperationException, IOException {
		this.response = httpResponse;
		this.jsonResponse = StringUtils.inputStreamToString(response.getEntity().getContent()); //This has to be done when we first get the response because once we read the stream, it is gone.
	}
	
	public void addJsonResponse(String jsonResponse) {
		this.jsonResponse = jsonResponse;
	}

	public String getResponse() throws ParseException, IOException {
		return jsonResponse;
	}
	
	public Integer getResponseCode() {
		return response.getStatusLine().getStatusCode();
	}
	
}
