package com.global.order_api.core.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.global.order_api.core.exception.FileStorageException;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FileUploadService {
	private final Cloudinary cloudinary;
	
	private final String FOLDER_NAME = "inventory-order-api/products";
	////UPLOAD IMAGE
	public String uploadImage(MultipartFile file)
	{
		try {
			
			Map uploadResult= cloudinary.uploader()
					// file.getbytes => image come from front-end and here
					// we convert it to bytes to transfer on web
					// make folder and give it name
					.upload(file.getBytes(), ObjectUtils.asMap(
							"folder",FOLDER_NAME));
			// cloudinary return map holds data about image uploaded
			// we get only the url of image
			return uploadResult.get("secure_url").toString();
		} catch (IOException e) {
            throw new FileStorageException("error.file.upload.failed");
        }
	}
	
	////DELETE IMAGE
	public void deleteImage(String imageUrl)
	{
		try
		{
			String publicId=extractPublicId(imageUrl);
			//	empty map => any additional options for delete process and singleton for all requests
			cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
		}
		catch (IOException e) {
            throw new FileStorageException("error.file.delete.failed");
        }
	}
	
	private String extractPublicId(String imageUrl)
	{
		// public id=> folder name + image name without .jpg
		// public id=> we use it to delete file not using the full url
		
		// split the full url to parts
		String[] parts=imageUrl.split("/");
		// get image name because it in last part of url
		String fileName=parts[parts.length-1];
		// remove .jpg from image name and add folder name
		return "FOLDER_NAME/"+ fileName.substring(0,fileName.lastIndexOf("."));
	}
}
