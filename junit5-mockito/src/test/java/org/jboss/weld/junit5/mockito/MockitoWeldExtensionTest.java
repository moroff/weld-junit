package org.jboss.weld.junit5.mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldJunit5Extension;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

//@ExtendWith(MockitoExtension.class)
@ExtendWith(WeldJunit5Extension.class)
@ExtendWith(MockitoWeldExtension.class)
class MockitoWeldExtensionTest {

	@WeldSetup
	WeldInitiator weldInstantor = WeldInitiator //
			.from(getClass(), ClassUnderTest.class)//
			.inject(this)//
			.build();

	@Inject
	ClassUnderTest classUnderTest;

	@Produces
	@ApplicationScoped
	TestInterface interfaceMock() {
		return mock(TestInterface.class, withSettings().verboseLogging());
	}

	@InjectMock
	TestInterface interfaceMock;
	
	@Test
	void testValid() {
		// Arrange
		when(interfaceMock.call("ValidParameter")).thenAnswer((i) -> {
			System.out.println("--> mock: " + interfaceMock.toString());
			return "ValidParameter";
		});

		// Act
		String result = classUnderTest.callInterface("ValidParameter");

		// Assert
		assertEquals("ValidParameter", result);
		
		verify(interfaceMock).call(Mockito.anyString());
	}

	@Test
	void testInvalid() {
		// Arrange
		when(interfaceMock.call("InvalidParameter")).thenThrow(IllegalArgumentException.class);

		// Act
		assertThrows(IllegalArgumentException.class, () -> {
			classUnderTest.callInterface("InvalidParameter");
		});

		// Assert
		verify(interfaceMock);
	}

}
