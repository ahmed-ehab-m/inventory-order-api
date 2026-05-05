package com.global.order_api;

import com.global.order_api.core.exception.ResourceNotFoundException;
import com.global.order_api.feature.category.*;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

/// run the project as we click on run button
/// for integration test
@SpringBootTest
class InventoryOrderApiApplicationTests {

	void contextLoads()
	{

	}

}
