package com.example.distribuidoraapp_semana6 // Asegúrate que el package name sea el correcto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Referencias a las vistas del XML
    private lateinit var editTextRegisterEmail: EditText
    private lateinit var editTextRegisterPassword: EditText
    private lateinit var editTextConfirmPassword: EditText
    private lateinit var buttonRegister: Button
    private lateinit var textViewLoginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register) // Enlaza con el XML que creaste

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Inicializar vistas
        editTextRegisterEmail = findViewById(R.id.editTextRegisterEmail)
        editTextRegisterPassword = findViewById(R.id.editTextRegisterPassword)
        editTextConfirmPassword = findViewById(R.id.editTextConfirmPassword)
        buttonRegister = findViewById(R.id.buttonRegister)
        textViewLoginLink = findViewById(R.id.textViewLoginLink)

        // Configurar Listener para el botón de registro
        buttonRegister.setOnClickListener {
            handleRegistration()
        }

        // Configurar Listener para el enlace de "Iniciar Sesión"
        textViewLoginLink.setOnClickListener {
            // Navegar de vuelta a LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            // Opcional: limpiar el stack para que el usuario no vuelva a RegisterActivity con el botón "atrás"
            // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Finaliza RegisterActivity
        }
    }

    private fun handleRegistration() {
        val email = editTextRegisterEmail.text.toString().trim()
        val password = editTextRegisterPassword.text.toString().trim()
        val confirmPassword = editTextConfirmPassword.text.toString().trim()

        // Validaciones
        if (!isValidEmail(email)) {
            editTextRegisterEmail.error = "Correo electrónico inválido."
            editTextRegisterEmail.requestFocus()
            return
        }

        if (password.length < 6) {
            editTextRegisterPassword.error = "La contraseña debe tener al menos 6 caracteres."
            editTextRegisterPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            editTextConfirmPassword.error = "Las contraseñas no coinciden."
            editTextConfirmPassword.requestFocus()
            // También podrías poner error en editTextRegisterPassword si quieres
            // editTextRegisterPassword.error = "Las contraseñas no coinciden."
            return
        }

        // Si todas las validaciones pasan, intentar crear el usuario en Firebase

        Log.d("RegisterActivity", "Intentando registrar usuario con correo: $email")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                // Aquí podrías ocultar el ProgressBar
                if (task.isSuccessful) {
                    // Registro exitoso
                    Log.d("RegisterActivity", "createUserWithEmail:success")
                    Toast.makeText(this, "Registro exitoso. Por favor, inicia sesión.", Toast.LENGTH_LONG).show()


                    // Navegar a LoginActivity después del registro exitoso
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK // Limpiar stack
                    startActivity(intent)
                    finish() // Finalizar RegisterActivity
                } else {
                    // Falló el registro
                    Log.w("RegisterActivity", "createUserWithEmail:failure", task.exception)
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        // El correo electrónico ya está en uso
                        editTextRegisterEmail.error = "Este correo electrónico ya está registrado."
                        editTextRegisterEmail.requestFocus()
                        Toast.makeText(baseContext, "Este correo electrónico ya está registrado.", Toast.LENGTH_LONG).show()
                    } else {
                        // Otro error
                        Toast.makeText(baseContext, "Fallo en el registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches() && email.isNotEmpty()
    }
}

