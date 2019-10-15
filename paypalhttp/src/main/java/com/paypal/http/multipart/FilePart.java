package com.paypal.http.multipart;

import com.paypal.http.serializer.Multipart;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.channels.Channels;

public class FilePart extends FormData {

	private File file;

	public FilePart(String key, File file) {
		super(key);
		this.file = file;
	}

	@Override
	public String header() {
		return super.header() + String.format("; filename=\"%s\"%sContent-Type: %s", file.getName(), Multipart.CRLF,
						URLConnection.guessContentTypeFromName(file.getName()));
	}

	@Override
	public void writeData(OutputStream os) throws IOException {
		try (FileInputStream fis = new FileInputStream(this.file)) {
			fis.getChannel().transferTo(0, this.file.length(), Channels.newChannel(os));
		}
	}
}
