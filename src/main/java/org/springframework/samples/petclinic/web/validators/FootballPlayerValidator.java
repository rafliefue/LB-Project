
package org.springframework.samples.petclinic.web.validators;

import java.util.Calendar;
import java.util.Date;

import org.springframework.samples.petclinic.model.FootballPlayer;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class FootballPlayerValidator implements Validator {

	private static final String REQUIRED = "required";


	@Override
	public void validate(final Object obj, final Errors errors) {
		FootballPlayer footballPlayer = (FootballPlayer) obj;
		String firstName = footballPlayer.getFirstName();
		String lastName = footballPlayer.getLastName();

		Date now = new Date(System.currentTimeMillis() - 1);

		Calendar cal = Calendar.getInstance();
		cal.setTime(now);
		cal.add(Calendar.YEAR, -16);

		now = cal.getTime();

		// First Name Validation
		if (!StringUtils.hasLength(firstName) || firstName.length() > 50 || firstName.length() < 2) {
			errors.rejectValue("firstName", "code.error.validator.requiredAndLength250", FootballPlayerValidator.REQUIRED + " and between 2 and 50 character");
		}

		// Last Name Validation
		if (!StringUtils.hasLength(lastName) || lastName.length() > 50 || lastName.length() < 2) {
			errors.rejectValue("lastName", "code.error.validator.requiredAndLength250", FootballPlayerValidator.REQUIRED + " and between 2 and 50 character");
		}

		// Position Validation
		if (footballPlayer.isNew() && footballPlayer.getPosition() == null) {
			errors.rejectValue("position", "code.error.validator.required", FootballPlayerValidator.REQUIRED);
		}

		// Birth Date Validation
		if (footballPlayer.getBirthDate() == null || footballPlayer.getBirthDate().after(now)) {
			errors.rejectValue("birthDate", "code.error.validator.requiredAnd16Years", FootballPlayerValidator.REQUIRED);
		}

	}

	@Override
	public boolean supports(final Class<?> clazz) {
		return FootballPlayer.class.isAssignableFrom(clazz);
	}

}
