package com.global.order_api.core.config;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.github.javafaker.Faker;
import com.global.order_api.feature.category.CategoryEntity;
import com.global.order_api.feature.category.CategoryRepo;
import com.global.order_api.feature.product.ProductEntity;
import com.global.order_api.feature.product.ProductRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Profile("dev") // Environment-based
@RequiredArgsConstructor
@Log4j2
//feature-toggle => used in seeder , sms because when i want to turn of i have not to change
// the whole environment
@ConditionalOnProperty(name = "app.seeder.enabled", havingValue = "true") 
public class DummyDataSeederConfig implements CommandLineRunner {
	
	private final CategoryRepo categoryRepo;
	private final ProductRepo productRepo;
	private final Faker faker;
	Long DEFAULT_CATEGORY_ID = 999L;
	
	// i uploaded it on Cloudinary
	private final List<String> DUMMY_IMAGES = List.of(
			"https://res.cloudinary.com/dsubioxtg/image/upload/q_auto/f_auto/v1775770716/sports_zkew8e.png",
			"https://res.cloudinary.com/dsubioxtg/image/upload/q_auto/f_auto/v1775770637/books_crfr8o.png",
			"https://res.cloudinary.com/dsubioxtg/image/upload/q_auto/f_auto/v1775770633/clothes_s4vpcb.png",
			"https://res.cloudinary.com/dsubioxtg/image/upload/q_auto/f_auto/v1775770619/homeandkitchen_rrznix.png",
			"https://res.cloudinary.com/dsubioxtg/image/upload/q_auto/f_auto/v1775770498/electronics_qsytfq.png"
	    );
	@Override
	public void run(String... args) throws Exception {
		
		if(productRepo.count() ==0)
		{
			log.info("🚀 Starting Product Seeding...");
			seedProducts();
		}
		else {
            log.info("⚡ Products already exist. Skipping seeding...");
        }
	}
	private void seedProducts()
	{
		List<CategoryEntity> existingCategories= categoryRepo.findAll();
		existingCategories.removeIf(category -> category.getId().equals(DEFAULT_CATEGORY_ID));
		if (existingCategories.isEmpty()) {
            log.error("❌ No categories found! Please add categories to the database first.");
            return;
        }
		Random random =new Random();
		// create random 50 product
		for(int i=0;i<50;i++)
		{
			ProductEntity product =new ProductEntity();
			product.setName(faker.commerce().productName());
			product.setDescription(faker.commerce().material() + " " + faker.lorem().paragraph());
			double randomPrice=10+(1000-10) * random.nextDouble();
			product.setPrice(BigDecimal.valueOf(randomPrice));
			product.setStockCount(faker.number().numberBetween(0, 100));
			String randomImage = DUMMY_IMAGES.get(random.nextInt(DUMMY_IMAGES.size()));
            product.setImage(randomImage);
            CategoryEntity randomCategory = existingCategories.get(random.nextInt(existingCategories.size()));
            product.setCategory(randomCategory);
            productRepo.save(product);
		}
	}
	
	
}
