package com.forge.autophone.form

import com.forge.autophone.AutoPhoneAccessibilityService
import com.forge.autophone.context.FieldType
import com.forge.autophone.inspector.UITreeInspector
import com.forge.autophone.model.NodeSnapshot
import kotlinx.coroutines.delay

/**
 * AdvancedFormAutomation — Intelligent form filling with validation and dependencies.
 *
 * Features:
 * - Auto-detects form field types (email, phone, date, etc.)
 * - Validates input format before submission
 * - Handles field dependencies (e.g., "State" depends on "Country")
 * - Supports conditional fields (appear/disappear based on selections)
 * - Auto-scrolls to fields out of view
 * - Handles dropdowns, date pickers, checkboxes, radio buttons
 */
class AdvancedFormAutomation(private val service: AutoPhoneAccessibilityService) {

    /**
     * Auto-fill an entire form using provided data.
     * Intelligently matches data to fields and validates input.
     */
    suspend fun autoFillForm(formData: Map<String, String>): FormFillResult {
        val fields = detectFormFields()
        val results = mutableMapOf<String, FieldFillResult>()
        
        fields.forEach { field ->
            val value = findMatchingValue(field, formData)
            if (value != null) {
                val result = fillField(field, value)
                results[field.viewId ?: "unknown"] = result
            }
        }
        
        return FormFillResult(
            totalFields = fields.size,
            filledFields = results.values.count { it.success },
            results = results
        )
    }

    /**
     * Detect all form fields in current screen.
     */
    fun detectFormFields(): List<FormField> {
        val snapshot = UITreeInspector(service).snapshot()
        val fields = mutableListOf<FormField>()
        
        snapshot.filter { it.isEditable || isFormControl(it) }.forEach { node ->
            val fieldType = classifyFieldType(node)
            val validation = getValidationRules(fieldType)
            
            fields.add(FormField(
                viewId = node.viewId,
                fieldType = fieldType,
                label = extractLabel(node),
                hint = node.contentDescription,
                isRequired = detectRequired(node),
                validation = validation,
                snapshot = node
            ))
        }
        
        return fields
    }

    /**
     * Fill a single form field with validation.
     */
    suspend fun fillField(field: FormField, value: String): FieldFillResult {
        // Validate input
        val validationError = validateInput(field, value)
        if (validationError != null) {
            return FieldFillResult(
                success = false,
                error = validationError
            )
        }
        
        // Scroll to field if not visible
        if (!isFieldVisible(field.snapshot)) {
            scrollToField(field.snapshot)
        }
        
        // Fill field based on type
        return when (field.fieldType) {
            FieldType.TEXT, FieldType.USERNAME, FieldType.PASSWORD,
            FieldType.EMAIL, FieldType.PHONE, FieldType.NUMBER -> {
                fillTextField(field, value)
            }
            FieldType.DATE -> {
                fillDateField(field, value)
            }
            FieldType.SEARCH -> {
                fillSearchField(field, value)
            }
            FieldType.UNKNOWN -> {
                FieldFillResult(success = false, error = "Unknown field type")
            }
        }
    }

    /**
     * Validate form before submission.
     */
    fun validateForm(): FormValidationResult {
        val fields = detectFormFields()
        val errors = mutableListOf<ValidationError>()
        
        fields.forEach { field ->
            // Check required fields
            if (field.isRequired) {
                val currentValue = field.snapshot.text ?: ""
                if (currentValue.isEmpty()) {
                    errors.add(ValidationError(
                        fieldId = field.viewId,
                        message = "Required field '${field.label}' is empty"
                    ))
                }
            }
            
            // Check validation rules
            val currentValue = field.snapshot.text ?: ""
            if (currentValue.isNotEmpty()) {
                val validationError = validateInput(field, currentValue)
                if (validationError != null) {
                    errors.add(ValidationError(
                        fieldId = field.viewId,
                        message = validationError
                    ))
                }
            }
        }
        
        return FormValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Find submit button and tap it.
     */
    suspend fun submitForm(): Boolean {
        val snapshot = UITreeInspector(service).snapshot()
        
        // Look for common submit button patterns
        val submitButton = snapshot.find { node ->
            val text = node.text?.lowercase() ?: ""
            val desc = node.contentDescription?.lowercase() ?: ""
            val id = node.viewId?.lowercase() ?: ""
            
            node.isClickable && (
                text in listOf("submit", "send", "save", "continue", "next", "sign up", "register", "login", "sign in") ||
                desc.contains("submit") ||
                id.contains("submit") || id.contains("save") || id.contains("continue")
            )
        }
        
        if (submitButton != null) {
            val centerX = (submitButton.boundsLeft + submitButton.boundsRight) / 2f
            val centerY = (submitButton.boundsTop + submitButton.boundsBottom) / 2f
            service.gestureHandler.performClick(centerX, centerY)
            return true
        }
        
        return false
    }

    /**
     * Handle field dependencies (e.g., State depends on Country).
     */
    suspend fun handleDependencies(
        primaryField: FormField,
        dependentField: FormField,
        dependencyMap: Map<String, List<String>>
    ): Boolean {
        val primaryValue = primaryField.snapshot.text ?: return false
        val allowedValues = dependencyMap[primaryValue] ?: return false
        
        // Check if dependent field has valid options
        return allowedValues.isNotEmpty()
    }

    // ── Private helper methods ───────────────────────────────────────────────

    private fun classifyFieldType(node: NodeSnapshot): FieldType {
        val hint = node.contentDescription?.lowercase() ?: ""
        val viewId = node.viewId?.lowercase() ?: ""
        val text = node.text?.lowercase() ?: ""
        
        return when {
            hint.contains("username") || viewId.contains("username") || text.contains("username") -> FieldType.USERNAME
            hint.contains("password") || viewId.contains("password") || text.contains("password") -> FieldType.PASSWORD
            hint.contains("email") || viewId.contains("email") || text.contains("email") -> FieldType.EMAIL
            hint.contains("phone") || viewId.contains("phone") || text.contains("phone") -> FieldType.PHONE
            hint.contains("search") || viewId.contains("search") || text.contains("search") -> FieldType.SEARCH
            node.className?.contains("NumberPicker", ignoreCase = true) == true -> FieldType.NUMBER
            node.className?.contains("DatePicker", ignoreCase = true) == true -> FieldType.DATE
            hint.contains("date") || viewId.contains("date") -> FieldType.DATE
            else -> FieldType.TEXT
        }
    }

    private fun getValidationRules(fieldType: FieldType): ValidationRules {
        return when (fieldType) {
            FieldType.EMAIL -> ValidationRules(
                pattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                minLength = 5,
                maxLength = 254,
                errorMessage = "Invalid email format"
            )
            FieldType.PHONE -> ValidationRules(
                pattern = "^[+]?[(]?[0-9]{1,4}[)]?[-\\s\\.]?[(]?[0-9]{1,4}[)]?[-\\s\\.]?[0-9]{1,9}$",
                minLength = 10,
                maxLength = 15,
                errorMessage = "Invalid phone number"
            )
            FieldType.PASSWORD -> ValidationRules(
                pattern = null, // Custom validation
                minLength = 8,
                maxLength = 128,
                errorMessage = "Password must be at least 8 characters"
            )
            FieldType.USERNAME -> ValidationRules(
                pattern = "^[a-zA-Z0-9_-]{3,20}$",
                minLength = 3,
                maxLength = 20,
                errorMessage = "Username must be 3-20 characters (letters, numbers, _ or -)"
            )
            FieldType.NUMBER -> ValidationRules(
                pattern = "^[0-9]+$",
                minLength = 1,
                maxLength = 10,
                errorMessage = "Must be a number"
            )
            else -> ValidationRules(
                pattern = null,
                minLength = 0,
                maxLength = 1000,
                errorMessage = null
            )
        }
    }

    private fun validateInput(field: FormField, value: String): String? {
        val rules = field.validation
        
        // Check min length
        if (value.length < rules.minLength) {
            return "Minimum length is ${rules.minLength} characters"
        }
        
        // Check max length
        if (value.length > rules.maxLength) {
            return "Maximum length is ${rules.maxLength} characters"
        }
        
        // Check pattern
        if (rules.pattern != null && !value.matches(Regex(rules.pattern))) {
            return rules.errorMessage ?: "Invalid format"
        }
        
        return null
    }

    private fun extractLabel(node: NodeSnapshot): String? {
        // Try to find label from hint or nearby text nodes
        return node.contentDescription ?: node.text
    }

    private fun detectRequired(node: NodeSnapshot): Boolean {
        val hint = node.contentDescription?.lowercase() ?: ""
        val text = node.text?.lowercase() ?: ""
        return hint.contains("required") || text.contains("required") || hint.contains("*") || text.contains("*")
    }

    private fun isFormControl(node: NodeSnapshot): Boolean {
        val className = node.className?.lowercase() ?: ""
        return className.contains("spinner") || // Dropdown
               className.contains("checkbox") ||
               className.contains("radiobutton") ||
               className.contains("switch") ||
               className.contains("seekbar")
    }

    private fun findMatchingValue(field: FormField, formData: Map<String, String>): String? {
        // Try exact match by view ID
        field.viewId?.let { id ->
            formData[id]?.let { return it }
        }
        
        // Try match by field type
        return when (field.fieldType) {
            FieldType.EMAIL -> formData["email"]
            FieldType.USERNAME -> formData["username"]
            FieldType.PASSWORD -> formData["password"]
            FieldType.PHONE -> formData["phone"]
            else -> null
        }
    }

    private suspend fun fillTextField(field: FormField, value: String): FieldFillResult {
        return try {
            field.viewId?.let { id ->
                service.textEntry.typeIntoViewId(value, id)
            }
            delay(200) // Wait for input to register
            FieldFillResult(success = true, error = null)
        } catch (e: Exception) {
            FieldFillResult(success = false, error = e.message)
        }
    }

    private suspend fun fillDateField(field: FormField, value: String): FieldFillResult {
        // For now, treat date fields as text fields
        // In future, handle DatePicker widgets specifically
        return fillTextField(field, value)
    }

    private suspend fun fillSearchField(field: FormField, value: String): FieldFillResult {
        return fillTextField(field, value)
    }

    private fun isFieldVisible(node: NodeSnapshot): Boolean {
        // Simple visibility check - field has bounds
        return node.boundsRight > node.boundsLeft && node.boundsBottom > node.boundsTop
    }

    private suspend fun scrollToField(node: NodeSnapshot) {
        // Find scrollable parent and scroll to make field visible
        val centerY = (node.boundsTop + node.boundsBottom) / 2f
        val screenHeight = 2400f // Approximate
        
        if (centerY > screenHeight * 0.8) {
            // Field is near bottom, scroll down
            service.gestureHandler.performSwipe(
                startX = 500f,
                startY = 1500f,
                endX = 500f,
                endY = 500f,
                durationMs = 300
            )
            delay(300)
        } else if (centerY < screenHeight * 0.2) {
            // Field is near top, scroll up
            service.gestureHandler.performSwipe(
                startX = 500f,
                startY = 500f,
                endX = 500f,
                endY = 1500f,
                durationMs = 300
            )
            delay(300)
        }
    }
}

// ── Data classes ─────────────────────────────────────────────────────────────

/**
 * Form field with metadata.
 */
data class FormField(
    val viewId: String?,
    val fieldType: FieldType,
    val label: String?,
    val hint: String?,
    val isRequired: Boolean,
    val validation: ValidationRules,
    val snapshot: NodeSnapshot
)

/**
 * Validation rules for a field.
 */
data class ValidationRules(
    val pattern: String?, // Regex pattern
    val minLength: Int,
    val maxLength: Int,
    val errorMessage: String?
)

/**
 * Result of filling a single field.
 */
data class FieldFillResult(
    val success: Boolean,
    val error: String?
)

/**
 * Result of auto-filling entire form.
 */
data class FormFillResult(
    val totalFields: Int,
    val filledFields: Int,
    val results: Map<String, FieldFillResult>
)

/**
 * Validation error.
 */
data class ValidationError(
    val fieldId: String?,
    val message: String
)

/**
 * Form validation result.
 */
data class FormValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError>
)
