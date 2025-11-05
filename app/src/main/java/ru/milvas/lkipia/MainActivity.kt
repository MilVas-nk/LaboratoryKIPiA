package ru.milvas.lkipia

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.content.Context
import android.view.inputmethod.InputMethodManager
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var vpiInput: TextInputEditText
    private lateinit var errorInput: TextInputEditText
    private lateinit var errorInputLayout: TextInputLayout
    private lateinit var typeRadioGroup: RadioGroup
    private lateinit var radioDatchik: RadioButton
    private lateinit var radioManometr: RadioButton
    private lateinit var checkButton: Button
    private lateinit var resultCard: LinearLayout
    private lateinit var resultTitle: TextView
    private lateinit var resultMessage: TextView
    private lateinit var resultDetails: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)
        initViews()
        setupListeners()
    }

    private fun initViews() {
        vpiInput = findViewById(R.id.vpiInput)
        errorInput = findViewById(R.id.errorInput)
        errorInputLayout = findViewById(R.id.errorInputLayout)
        typeRadioGroup = findViewById(R.id.typeRadioGroup)
        radioDatchik = findViewById(R.id.radioDatchik)
        radioManometr = findViewById(R.id.radioManometr)
        checkButton = findViewById(R.id.checkButton)
        resultCard = findViewById(R.id.resultCard)
        resultTitle = findViewById(R.id.resultTitle)
        resultMessage = findViewById(R.id.resultMessage)
        resultDetails = findViewById(R.id.resultDetails)
        resultCard.visibility = LinearLayout.GONE
    }

    private fun setupListeners() {
        typeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            resultCard.visibility = LinearLayout.GONE
            when (checkedId) {
                R.id.radioDatchik -> {
                    errorInputLayout.hint = "Погрешность (%)"
                }
                R.id.radioManometr -> {
                    errorInputLayout.hint = "Класс точности"
                }
            }
        }
        checkButton.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            performCheck()
        }
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                resultCard.visibility = LinearLayout.GONE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        }
        vpiInput.addTextChangedListener(textWatcher)
        errorInput.addTextChangedListener(textWatcher)
    }

    private fun performCheck() {
        val vpiStr = vpiInput.text.toString()
        val errorStr = errorInput.text.toString()
        if (vpiStr.isEmpty() || errorStr.isEmpty()) {
            showError("Заполните все поля")
            return
        }
        val vpi = vpiStr.toDoubleOrNull()
        val error = errorStr.toDoubleOrNull()
        if (vpi == null || error == null) {
            showError("Введите корректные числовые значения")
            return
        }
        // Убираем ограничение на отрицательные и нулевые значения ВПИ — это покрыто диапазонами ниже
        if (error <= 0) {
            val errorText = if (radioDatchik.isChecked) "Погрешность" else "Класс точности"
            showError("$errorText должен быть положительным")
            return
        }
        val deviceType = if (radioDatchik.isChecked) DeviceType.DATCHIK else DeviceType.MANOMETR
        val result = checkAccreditation(vpi, error, deviceType)
        showResult(result)
    }

    private fun checkAccreditation(
        vpi: Double,
        error: Double,
        type: DeviceType
    ): CheckResult {
        val vpiRanges = listOf(
            VpiRange(-0.1, 0.0, "((-0.1) - 0) МПа"),
            VpiRange(0.004, 0.160, "(0.004 - 0.160 МПа"),
            VpiRange(0.06, 0.6, "(0.06 - 0.6) МПа"),
            VpiRange(0.16, 0.6, "(0.16 - 0.6) МПа"),
            VpiRange(0.4, 1.6, "(0.4 - 1.6) МПа"),
            VpiRange(1.0, 6.0, "(1 - 6) МПа"),
            VpiRange(6.0, 10.0, "(6 - 10) МПа"),
            VpiRange(10.0, 60.0, "(10 - 60) МПа")
        )
        val errorName = if (type == DeviceType.DATCHIK) "погрешность" else "класс точности"
        val deviceName = if (type == DeviceType.DATCHIK) "Датчик давления" else "Манометр"
        val errorUnit = if (type == DeviceType.DATCHIK) "%" else ""
        val matchingRanges = vpiRanges.filter { range -> vpi >= range.min && vpi <= range.max }
        if (matchingRanges.isEmpty()) {
            return CheckResult(
                canVerify = false,
                message = "Нельзя поверить",
                details = "$deviceName с ВПИ $vpi МПа не попадает ни в один из диапазонов аккредитации лаборатории"
            )
        }
        for (range in matchingRanges) {
            val (minError, maxError) = when {
                type == DeviceType.MANOMETR -> 0.6 to Double.MAX_VALUE
                range.name == "((-0.1) - 0) МПа" -> 0.25 to 3.0
                range.name == "(0.004 - 0.160) МПа" -> 0.25 to 3.0
                range.name == "(0.06 - 0.6) МПа" -> 0.15 to 3.0
                range.name == "(0.16 - 0.6) МПа" -> 0.25 to 3.0
                range.name == "(0.4 - 1.6) МПа" -> 0.25 to 3.0
                range.name == "(1 - 6) МПа" -> 0.1 to 3.0
                range.name == "(6 - 10) МПа" -> 0.25 to 3.0
                range.name == "(10 - 60) МПа" -> 0.15 to 3.0
                else -> 0.25 to 3.0
            }
            if (type == DeviceType.DATCHIK) {
                if (error >= minError && error <= maxError) {
                    return CheckResult(
                        canVerify = true,
                        message = "Можно поверить",
                        details = "Диапазон: ВПИ ${range.name}\n" +
                                "ВПИ прибора: $vpi МПа\n" +
                                "${errorName.replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) }}: $error $errorUnit" // (требуется $minError-$maxError$errorUnit)"
                    )
                }
            } else {
                if (error >= minError) {
                    return CheckResult(
                        canVerify = true,
                        message = "Можно поверить",
                        details = "Диапазон: ВПИ ${range.name}\n" +
                                "ВПИ прибора: $vpi МПа\n" +
                                "${errorName.replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) }}: $error" // (требуется ≥ $minError)"
                    )
                }
            }
        }
        val range = matchingRanges.first()
        if (type == DeviceType.DATCHIK) {
            val (minError, maxError) = when (range.name) {
                "((-0.1) - 0) МПа" -> 0.25 to 3.0
                "(0.004 - 0.160) МПа" -> 0.25 to 3.0
                "(0.06 - 0.6) МПа" -> 0.15 to 3.0
                "(0.16 - 0.6) МПа" -> 0.25 to 3.0
                "(0.4 - 1.6) МПа" -> 0.25 to 3.0
                "(1 - 6) МПа" -> 0.1 to 3.0
                "(6 - 10) МПа" -> 0.25 to 3.0
                "(10 - 60) МПа" -> 0.15 to 3.0
                else -> 0.25 to 3.0
            }
            return if (error < minError) {
                CheckResult(
                    canVerify = false,
                    message = "Нельзя поверить",
                    details = "${errorName.replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) }} прибора $error $errorUnit меньше минимально допустимой\n" +
                            "Для диапазона ${range.name} требуется ${errorName} от $minError $errorUnit до $maxError $errorUnit"
                )
            } else {
                CheckResult(
                    canVerify = false,
                    message = "Нельзя поверить",
                    details = "${errorName.replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) }} прибора $error $errorUnit больше максимально допустимой\n" +
                            "Для диапазона ${range.name} требуется ${errorName} от $minError $errorUnit до $maxError $errorUnit"
                )
            }
        } else {
            return CheckResult(
                canVerify = false,
                message = "Нельзя поверить",
                details = "${errorName.replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) }} манометра $error меньше минимально допустимого\n" +
                        "Для всех диапазонов требуется ${errorName} ≥ 0.6"
            )
        }
    }

    private fun showResult(result: CheckResult) {
        resultCard.visibility = LinearLayout.VISIBLE
        if (result.canVerify) {
            resultCard.setBackgroundColor(ContextCompat.getColor(this, R.color.success_bg))
            resultTitle.setTextColor(ContextCompat.getColor(this, R.color.success_text))
            resultTitle.text = "✓ ${result.message}"
        } else {
            resultCard.setBackgroundColor(ContextCompat.getColor(this, R.color.error_bg))
            resultTitle.setTextColor(ContextCompat.getColor(this, R.color.error_text))
            resultTitle.text = "✗ ${result.message}"
        }
        resultMessage.text = result.details
        val deviceType = if (radioDatchik.isChecked) "Датчик давления" else "Манометр"
        resultDetails.text = "Тип прибора: $deviceType"
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

enum class DeviceType {
    DATCHIK,
    MANOMETR
}

data class VpiRange(
    val min: Double,
    val max: Double,
    val name: String
)

data class CheckResult(
    val canVerify: Boolean,
    val message: String,
    val details: String
)