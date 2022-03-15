package org.jboss.weld.junit5.mockito;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jboss.weld.proxy.WeldClientProxy;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mock;

public class MockitoWeldExtension implements BeforeAllCallback, BeforeEachCallback {
//	private static Logger LOGGER = Utils.setupAndCreateLogger(MockitoWeldExtension.class);
	
	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		if (determineTestLifecycle(context).equals(Lifecycle.PER_METHOD)) {
			addMockToInjectInto(context);
		}

	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		if (determineTestLifecycle(context).equals(Lifecycle.PER_CLASS)) {
			addMockToInjectInto(context);
		}
	}

	private void addMockToInjectInto(ExtensionContext context) throws Exception {
//			 LOGGER.info("Inject mockito mocks to weld");
		WeldInitiator weldInitiator = determineWeldInitiator(context);
		handleInjectMocks(context, weldInitiator);
	}

	private void handleInjectMocks(ExtensionContext context, WeldInitiator weldInitiator) throws Exception {
		Object testInstance = context.getTestInstance().orElseGet(null);
		if (testInstance == null) {
			throw new IllegalStateException("ExtensionContext.getTestInstance() returned empty Optional!");
		}
		// all found fields which are WeldInitiator and have @Mock annotation
		List<Object> foundMocks = new ArrayList<>();
		// We will go through class hierarchy in search of @WeldSetup field (even
		// private)
		for (Class<?> clazz = testInstance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
			// Find @WeldSetup field using getDeclaredFields() - this allows even for
			// private fields
			for (Field field : clazz.getDeclaredFields()) {
				if (field.isAnnotationPresent(InjectMock.class)) {
					Class<?> fieldType = field.getType();
					Object mockInstance = ((WeldClientProxy)weldInitiator.getBeanManager().createInstance().select(fieldType).get()).getMetadata().getContextualInstance();
					try {
						field.set(testInstance, mockInstance);
					} catch (IllegalAccessException e) {
						// In case we cannot get to the field, we need to set accessibility as well
						AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
							field.setAccessible(true);
							return null;
						});
						field.set(testInstance, mockInstance);
					}
				}
			}
		}
	}

	private TestInstance.Lifecycle determineTestLifecycle(ExtensionContext ec) {
		// check the test for org.junit.jupiter.api.TestInstance annotation
		TestInstance annotation = ec.getRequiredTestClass().getAnnotation(TestInstance.class);
		if (annotation != null) {
			return annotation.value();
		} else {
			return TestInstance.Lifecycle.PER_METHOD;
		}
	}

	private WeldInitiator determineWeldInitiator(ExtensionContext context) throws Exception {
		Object testInstance = context.getTestInstance().orElseGet(null);
		if (testInstance == null) {
			throw new IllegalStateException("ExtensionContext.getTestInstance() returned empty Optional!");
		}

		// all found fields which are WeldInitiator and have @WeldSetup annotation
		List<Field> foundInitiatorFields = new ArrayList<>();
		WeldInitiator initiator = null;
		// We will go through class hierarchy in search of @WeldSetup field (even
		// private)
		for (Class<?> clazz = testInstance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
			// Find @WeldSetup field using getDeclaredFields() - this allows even for
			// private fields
			for (Field field : clazz.getDeclaredFields()) {
				if (field.isAnnotationPresent(WeldSetup.class)) {
					Object fieldInstance;
					try {
						fieldInstance = field.get(testInstance);
					} catch (IllegalAccessException e) {
						// In case we cannot get to the field, we need to set accessibility as well
						AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
							field.setAccessible(true);
							return null;
						});
						fieldInstance = field.get(testInstance);
					}
					if (fieldInstance != null && fieldInstance instanceof WeldInitiator) {
						initiator = (WeldInitiator) fieldInstance;
						foundInitiatorFields.add(field);
					} else {
						// Field with other type than WeldInitiator was annotated with @WeldSetup
						throw new IllegalStateException("@WeldSetup annotation should only be used on a field of type"
								+ " WeldInitiator but was found on a field of type " + field.getType()
								+ " which is declared " + "in class " + field.getDeclaringClass());
					}
				}
			}
		}
		// Multiple occurrences of @WeldSetup in the hierarchy will lead to an exception
		if (foundInitiatorFields.size() > 1) {
			throw new IllegalStateException(foundInitiatorFields.stream()
					.map(f -> "Field type - " + f.getType() + " which is " + "in " + f.getDeclaringClass())
					.collect(Collectors.joining("\n",
							"Multiple @WeldSetup annotated fields found, " + "only one is allowed! Fields found:\n",
							"")));
		}

		return (WeldInitiator) foundInitiatorFields.get(0).get(testInstance);
	}

}
