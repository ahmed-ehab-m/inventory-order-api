package com.global.order_api.core.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.global.order_api.core.exception.FileStorageException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FileUploadService {
	private final Cloudinary cloudinary;
	
	private final String FOLDER_NAME = "inventory-order-api/products";
	
	private final List<String> ALLOWED_IMAGE_TYPES=List.of("image/jpeg","image/jpg", "image/png", "image/webp");
	private final long MAX_FILE_SIZE = 5 * 1024 * 1024;
	////UPLOAD IMAGE
	public String uploadImage(MultipartFile file)
	{
		validateImage(file);
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
		return FOLDER_NAME+"/"+ fileName.substring(0,fileName.lastIndexOf("."));
	}
	/////////
	private void validateImage(MultipartFile file)
	{
		if(file.isEmpty())
		{
			throw new IllegalArgumentException("الملف فارغ لا يمكن رفعه");	
		}
		// IMAGE SIZE
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new IllegalArgumentException("حجم الصورة يجب ألا يتعدى 5 ميجا بايت");
		}
		// IMAGE TYPE
		// getcontenttype not getExtension for more security
		// because hacker can pass a virus script and name it like virus.jpg
		// but here we read MIME Type
		//
		if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
			throw new IllegalArgumentException("النوع غير مدعوم! مسموح فقط بـ (JPG, PNG, WEBP)");
		}
	}
}
