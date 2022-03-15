package org.jboss.weld.junit5.mockito;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

public class ClassUnderTest {

	@Inject // @Dependent
	TestInterface testInterface;
	
	public String callInterface(String parameter) {
		return testInterface.call(parameter);
	}
	
}
