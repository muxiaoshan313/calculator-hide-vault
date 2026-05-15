package calculator.hide.vaultpro.config

/**
 * Central configuration for calculator secret formula entries.
 * Formulas are matched when the user presses "=" (the "=" is appended for matching).
 */
object SecretFormulaConfig {

    const val FORMULA_PRIVATE_APP_MANAGER = "123+="
    const val FORMULA_PUBLIC_DESKTOP = "456+="
    const val FORMULA_PRIVATE_DESKTOP = "789+="

    /** @deprecated Use [FORMULA_PRIVATE_APP_MANAGER] */
    const val FORMULA_APP_MANAGER = FORMULA_PRIVATE_APP_MANAGER

    /** @deprecated Use [FORMULA_PUBLIC_DESKTOP] */
    const val FORMULA_PUBLIC_LAUNCHER = FORMULA_PUBLIC_DESKTOP

    /** @deprecated Use [FORMULA_PRIVATE_DESKTOP] */
    const val FORMULA_PRIVATE_LAUNCHER = FORMULA_PRIVATE_DESKTOP

    /** Vault (photos/videos) — long-press "=" after entering this code */
    const val FORMULA_VAULT = "123456"

    enum class Destination {
        PRIVATE_APP_MANAGER,
        PUBLIC_DESKTOP,
        PRIVATE_DESKTOP
    }

    /**
     * @param input Current calculator input when "=" is pressed (without trailing "=")
     */
    fun matchDestination(input: String): Destination? {
        val formula = if (input.endsWith("=")) input else "$input="
        return when (formula) {
            FORMULA_PRIVATE_APP_MANAGER -> Destination.PRIVATE_APP_MANAGER
            FORMULA_PUBLIC_DESKTOP -> Destination.PUBLIC_DESKTOP
            FORMULA_PRIVATE_DESKTOP -> Destination.PRIVATE_DESKTOP
            else -> null
        }
    }
}
