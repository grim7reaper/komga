package org.gotson.komga.interfaces.rest.dto

import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.constraints.Positive
import kotlin.reflect.KClass

@ReadProgressUpdateDtoConstraint
data class ReadProgressUpdateDto(
  @get:Positive val page: Int?,
  val completed: Boolean?
)

@Constraint(validatedBy = [ReadProgressUpdateDtoValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReadProgressUpdateDtoConstraint(
  val message: String = "page must be specified if completed is false or null",
  val groups: Array<KClass<out Any>> = [],
  val payload: Array<KClass<out Any>> = []
)

class ReadProgressUpdateDtoValidator : ConstraintValidator<ReadProgressUpdateDtoConstraint, ReadProgressUpdateDto> {
  override fun isValid(value: ReadProgressUpdateDto?, context: ConstraintValidatorContext?): Boolean =
    value != null && (
      value.page != null || (value.completed != null && value.completed)
      )
}
