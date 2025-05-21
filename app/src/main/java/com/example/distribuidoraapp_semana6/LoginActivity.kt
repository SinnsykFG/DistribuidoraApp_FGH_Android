package com.example.distribuidoraapp_semana6

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Referencias a las vistas del XML
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLoginEmail: Button
    private lateinit var buttonRegisterEmail: Button
    private lateinit var googleSignInButton: SignInButton // Ya la tenías, pero la declaro aquí para consistencia

    // Lanzador para el resultado del inicio de sesión con Google
    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    if (account != null && account.idToken != null) {
                        firebaseAuthWithGoogle(account.idToken!!)
                    } else {
                        Log.e("LoginActivity", "Error: La cuenta de Google o el idToken es nulo.")
                        Toast.makeText(this, "No se pudo obtener el token de Google.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: ApiException) {
                    Log.e("LoginActivity", "Error en el inicio de sesión con Google. Código: ${e.statusCode}", e)
                    Toast.makeText(this, "Error en el inicio de sesión con Google. Código: ${e.statusCode}", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.w("LoginActivity", "El inicio de sesión con Google fue cancelado o falló. Código de resultado: ${result.resultCode}")
                Toast.makeText(this, "Inicio de sesión con Google cancelado.", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login) // Asegúrate que tu XML se llama activity_login.xml

        Log.d("LoginActivity", "onCreate: Inicializando LoginActivity")

        // Configura Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Inicializar vistas del XML
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonLoginEmail = findViewById(R.id.buttonLoginEmail)
        buttonRegisterEmail = findViewById(R.id.buttonRegisterEmail)
        googleSignInButton = findViewById(R.id.googleSignInButton) // El ID que usaste en tu XML

        // Configura Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Crucial: verifica este ID
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // --- Configurar Listeners para los botones ---

        // Listener para el botón de INICIAR SESIÓN CON CORREO
        buttonLoginEmail.setOnClickListener {
            handleEmailLogin()
        }

        // Listener para el botón de REGISTRARSE CON CORREO
        buttonRegisterEmail.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Listener para el botón de INICIAR SESIÓN CON GOOGLE
        googleSignInButton.setSize(SignInButton.SIZE_WIDE) // Opcional: ajusta el tamaño del botón
        googleSignInButton.setOnClickListener {
            Log.d("LoginActivity", "Botón de inicio de sesión con Google presionado.")
            signInWithGoogle()
        }
    }

    override fun onStart() {
        super.onStart()
        // Si ya hay una sesión activa, salta directamente a MenuActivity
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("LoginActivity", "Usuario ya autenticado: ${currentUser.email}. Navegando a MenuActivity.")
            navigateToMenuActivity()
        } else {
            Log.d("LoginActivity", "No hay usuario autenticado.")
        }
    }

    // --- Funciones para Email/Contraseña ---

    private fun handleEmailLogin() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (!isValidEmail(email)) {
            editTextEmail.error = "Correo electrónico inválido."
            editTextEmail.requestFocus()
            return
        }
        if (password.isEmpty()) {
            editTextPassword.error = "La contraseña no puede estar vacía."
            editTextPassword.requestFocus()
            return
        }
        // Podrías añadir un ProgressBar aquí para feedback visual
        Log.d("LoginActivity", "Intentando iniciar sesión con correo: $email")
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "signInWithEmail:success")
                    Toast.makeText(this, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()
                    navigateToMenuActivity()
                } else {
                    Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Fallo de autenticación: ${task.exception?.message}",
                        Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleEmailRegister() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (!isValidEmail(email)) {
            editTextEmail.error = "Correo electrónico inválido."
            editTextEmail.requestFocus()
            return
        }
        if (password.length < 6) { // Firebase requiere al menos 6 caracteres para la contraseña
            editTextPassword.error = "La contraseña debe tener al menos 6 caracteres."
            editTextPassword.requestFocus()
            return
        }
        // Podrías añadir un ProgressBar aquí
        Log.d("LoginActivity", "Intentando registrar con correo: $email")
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "createUserWithEmail:success")
                    Toast.makeText(this, "Registro exitoso. Por favor, inicia sesión.", Toast.LENGTH_LONG).show()
                    // Opcional: podrías iniciar sesión directamente aquí y navegar a MenuActivity
                    // o pedir al usuario que verifique su email si implementas esa funcionalidad.
                    // Por ahora, solo mostramos mensaje y el usuario puede loguearse.
                    auth.signOut() // Para que onStart no lo loguee automáticamente sin que el usuario presione "Iniciar Sesión"
                    editTextPassword.text.clear() // Limpiar contraseña por seguridad/UX
                } else {
                    Log.w("LoginActivity", "createUserWithEmail:failure", task.exception)
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(baseContext, "Este correo electrónico ya está registrado.",
                            Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(baseContext, "Fallo en el registro: ${task.exception?.message}",
                            Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches() && email.isNotEmpty()
    }

    // --- Funciones para Google Sign-In (ya las tenías y están bien) ---

    private fun signInWithGoogle() {
        Log.d("LoginActivity", "Iniciando el flujo de inicio de sesión con Google.")
        val signInIntent = googleSignInClient.signInIntent
        try {
            googleSignInLauncher.launch(signInIntent)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error al lanzar signInIntent", e)
            Toast.makeText(this, "Error al iniciar Google Sign-In. Verifica Google Play Services.", Toast.LENGTH_LONG).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d("LoginActivity", "Autenticando con Firebase usando el token de Google.")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("LoginActivity", "signInWithCredential (Google) exitoso.")
                    Toast.makeText(this, "Inicio de sesión con Google exitoso.", Toast.LENGTH_SHORT).show()
                    navigateToMenuActivity()
                } else {
                    Log.e("LoginActivity", "signInWithCredential (Google) falló", task.exception)
                    Toast.makeText(this, "Error de autenticación con Google: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // --- Navegación ---
    private fun navigateToMenuActivity() {
        val intent = Intent(this, MenuActivity::class.java)
        // Opcional: limpiar el stack de actividades para que el usuario no vuelva a LoginActivity con el botón "atrás"
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Finaliza LoginActivity
    }
}