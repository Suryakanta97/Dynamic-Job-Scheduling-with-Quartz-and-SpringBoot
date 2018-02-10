package com.surya.quartz.model;

import static java.time.ZoneId.systemDefault;
import static java.util.UUID.randomUUID;
import static org.quartz.CronExpression.isValidExpression;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.springframework.util.StringUtils.isEmpty;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.TimeZone;

import org.hibernate.validator.constraints.NotBlank;
import org.quartz.JobDataMap;
import org.quartz.Trigger;

import lombok.Data;

@Data
public class TriggerDescriptor {
	@NotBlank
	private String name;
	private String group;
	private LocalDateTime fireTime;
	private String cron;

	public TriggerDescriptor setName(final String name) {
		this.name = name;
		return this;
	}

	public TriggerDescriptor setGroup(final String group) {
		this.group = group;
		return this;
	}

	public TriggerDescriptor setFireTime(final LocalDateTime fireTime) {
		this.fireTime = fireTime;
		return this;
	}

	public TriggerDescriptor setCron(final String cron) {
		this.cron = cron;
		return this;
	}

	private String buildName() {
		return isEmpty(name) ? randomUUID().toString() : name;
	}

	/**
	 * Convenience method for building a Trigger
	 * 
	 * @return the Trigger associated with this descriptor
	 */
	public Trigger buildTrigger() {
		// @formatter:off
		if (!isEmpty(cron)) {
			if (!isValidExpression(cron))
				throw new IllegalArgumentException("Provided expression " + cron + " is not a valid cron expression");
			return newTrigger()
					.withIdentity(buildName(), group)
					.withSchedule(cronSchedule(cron)
							.withMisfireHandlingInstructionFireAndProceed()
							.inTimeZone(TimeZone.getTimeZone(systemDefault())))
					.usingJobData("cron", cron)
					.build();
		} else if (!isEmpty(fireTime)) {
			JobDataMap jobDataMap = new JobDataMap();
			jobDataMap.put("fireTime", fireTime);
			return newTrigger()
					.withIdentity(buildName(), group)
					.withSchedule(simpleSchedule()
							.withMisfireHandlingInstructionNextWithExistingCount())
					.startAt(Date.from(fireTime.atZone(systemDefault()).toInstant()))
					.usingJobData(jobDataMap)
					.build();
		}
		// @formatter:on
		throw new IllegalStateException("unsupported trigger descriptor " + this);
	}

	/**
	 * 
	 * @param trigger
	 *            the Trigger used to build this descriptor
	 * @return the TriggerDescriptor
	 */
	public static TriggerDescriptor buildDescriptor(Trigger trigger) {
		// @formatter:off
		return new TriggerDescriptor()
				.setName(trigger.getKey().getName())
				.setGroup(trigger.getKey().getGroup())
				.setFireTime((LocalDateTime) trigger.getJobDataMap().get("fireTime"))
				.setCron(trigger.getJobDataMap().getString("cron"));
		// @formatter:on
	}
}
