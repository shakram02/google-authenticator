package shakram02.ahmed.lwa

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import shakram02.ahmed.lwa.otp.Base32String
import shakram02.ahmed.lwa.otp.OtpSettingsStore
import shakram02.ahmed.lwa.otp.OtpType


class KeyGenerationActivity : Activity(), TextWatcher {
    private lateinit var mKeyEntryField: EditText
    private lateinit var secretManager: OtpSettingsStore
    private lateinit var mType: Spinner

    companion object {
        private const val MIN_KEY_BYTES = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_key_genration)
        setupUiEventHandlers()

        val packageName = this.applicationContext.packageName
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        secretManager = OtpSettingsStore(sharedPref, packageName)

        if (secretManager.canLoad()) {
            disableKeySubmitButton()
            showShortToast("Key loaded")
        }
    }

    private fun validateKey(submitting: Boolean): Boolean {
        val userEnteredKey = getEnteredKey()

        return try {
            val decoded = Base32String.decode(userEnteredKey)
            if (decoded.size < MIN_KEY_BYTES) {
                // If the user is trying to submit a key that's too short, then
                // display a message saying it's too short.
                mKeyEntryField.error = if (submitting) getString(R.string.enter_key_too_short) else null
                false
            } else {
                mKeyEntryField.error = null
                true
            }
        } catch (e: Base32String.DecodingException) {
            mKeyEntryField.error = getString(R.string.enter_key_illegal_char)
            false
        }
    }

    private fun performKeySubmission() {
        // Store in preferences
        storeSecretPref()
        disableKeySubmitButton()
        showShortToast("Key saved")
    }

    /*
     * Return key entered by user, replacing visually similar characters 1 and 0.
     * They're removed probably because user confusion when copying keys to phones
     * RFC 4648/3548
     **/
    private fun getEnteredKey(): String {
        val enteredKey = mKeyEntryField.text.toString()
        return enteredKey.replace('1', 'I').replace('0', 'O')
    }

    override fun afterTextChanged(s: Editable?) {
        validateKey(false)
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        // Nothing
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        // Nothing
    }

    private fun disableKeySubmitButton() {
        findViewById<Button>(R.id.key_submit_button).isEnabled = false
    }

    private fun showShortToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun storeSecretPref() {
        // TODO(cemp): This depends on the OtpType enumeration to correspond
        // to array indices for the dropdown with different OTP modes.
        val mode = if (mType.selectedItemPosition == OtpType.TOTP.value)
            OtpType.TOTP
        else
            OtpType.HOTP

        if (mode == OtpType.TOTP) {
            secretManager.save(getEnteredKey(), mode)
        } else {
            secretManager.save(getEnteredKey(), mode, 0)
        }
    }

    private fun setupUiEventHandlers() {
        mKeyEntryField = findViewById(R.id.key_value)
        mKeyEntryField.addTextChangedListener(this)

        findViewById<Button>(R.id.key_submit_button)
                .setOnClickListener { if (validateKey(true)) performKeySubmission() }

        mType = findViewById(R.id.type_choice)
        val types = ArrayAdapter.createFromResource(this,
                R.array.type, android.R.layout.simple_spinner_item)
        types.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mType.adapter = types
    }
}
