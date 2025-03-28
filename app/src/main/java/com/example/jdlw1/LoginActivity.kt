package com.example.jdlw1

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.example.jdlw1.ui.theme.JDLW1Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Función para encriptar contraseñas antes de guardarlas en Firestore.
 */
fun encryptPassword(password: String, secretKey: String = "MySecretKey123456"): String {
    val keyBytes = secretKey.toByteArray(Charsets.UTF_8).copyOf(16)
    val keySpec = SecretKeySpec(keyBytes, "AES")
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, keySpec)
    val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(encrypted, Base64.DEFAULT).trim()
}

/**
 * Actividad principal para Login y Registro.
 */
class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()

        // Si el usuario ya está autenticado, redirige directamente a MainActivity
        if (auth.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            JDLW1Theme {
                var showRegistration by remember { mutableStateOf(false) }
                if (showRegistration) {
                    RegistrationScreen(
                        onRegistrationSuccess = {
                            Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                            showRegistration = false
                        },
                        onNavigateToLogin = { showRegistration = false }
                    )
                } else {
                    LoginScreen(
                        onLoginSuccess = {
                            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        },
                        onNavigateToRegister = { showRegistration = true }
                    )
                }
            }
        }
    }
}

/**
 * Pantalla de Inicio de Sesión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

    val isInPreview = LocalInspectionMode.current
    val auth = if (!isInPreview) FirebaseAuth.getInstance() else null

    // Se centra todo el contenido en pantalla
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título
        Text(
            text = "Iniciar sesión",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111418),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Campo: Correo electrónico
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Correo electrónico", color = Color(0xFF637588)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF0F2F4),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )

        // Campo: Contraseña con mostrar/ocultar
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Contraseña", color = Color(0xFF637588)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF0F2F4),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )

        // Botón "Olvidé mi contraseña"
        TextButton(
            onClick = {
                if (email.isNotBlank()) {
                    auth?.sendPasswordResetEmail(email)
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Correo de restauración enviado", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error al enviar correo de restauración", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(context, "Ingrese su correo para restaurar la contraseña", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Olvidé mi contraseña",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111418)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón "Iniciar sesión"
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Correo y contraseña son obligatorios", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                auth?.signInWithEmailAndPassword(email, password)
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            onLoginSuccess()
                        } else {
                            Toast.makeText(context, "Correo o contraseña incorrectos", Toast.LENGTH_SHORT).show()
                            loginError = task.exception?.message ?: "Error de autenticación"
                        }
                    } ?: onLoginSuccess()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1980E6)),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = "Iniciar sesión",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Link a registro
        Text(
            text = "No tienes cuenta? Regístrate",
            color = Color(0xFF637588),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { onNavigateToRegister() }
        )
    }
}

/**
 * Preview de la pantalla de Login.
 * Debe ser top-level para que Android Studio la muestre correctamente.
 */
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    JDLW1Theme {
        LoginScreen()
    }
}

/**
 * Pantalla de Registro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(
    onRegistrationSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var registrationError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val isInPreview = LocalInspectionMode.current
    val auth = if (!isInPreview) FirebaseAuth.getInstance() else null

    // Se centra el contenido y se añade scroll para evitar cortes en pantallas pequeñas
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Registro",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111418),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Campo: Nombre
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Nombre", color = Color(0xFF637588)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF0F2F4),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )

        // Campo: Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Correo electrónico", color = Color(0xFF637588)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF0F2F4),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )

        // Campo: Contraseña
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Contraseña", color = Color(0xFF637588)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF0F2F4),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )

        // Campo: Confirmar Contraseña
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            placeholder = { Text("Confirmar Contraseña", color = Color(0xFF637588)) },
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (confirmPasswordVisible) "Ocultar" else "Mostrar"
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0xFFF0F2F4),
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )

        // Mensaje de error (si existe)
        registrationError?.let { errorMsg ->
            Text(
                text = errorMsg,
                color = Color.Red,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón "Registrarse"
        Button(
            onClick = {
                // Primero, validamos que todos los campos estén llenos
                if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                    registrationError = "Todos los campos son obligatorios."
                    return@Button
                }

                // Luego, validamos que las contraseñas coincidan
                if (password != confirmPassword) {
                    registrationError = "Las contraseñas no coinciden."
                } else {
                    if (auth != null) {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val uid = auth.currentUser?.uid
                                    if (uid != null) {
                                        val db = Firebase.firestore
                                        try {
                                            val encryptedPassword = encryptPassword(password)
                                            val userData = hashMapOf(
                                                "name" to name,
                                                "email" to email,
                                                "password" to encryptedPassword
                                            )
                                            db.collection("users").document(uid)
                                                .set(userData)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                                    onRegistrationSuccess()
                                                }
                                                .addOnFailureListener { e ->
                                                    registrationError = "Error guardando datos: ${e.message}"
                                                }
                                        } catch (e: Exception) {
                                            registrationError = "Error al encriptar o guardar datos: ${e.message}"
                                            e.printStackTrace()
                                        }
                                    } else {
                                        registrationError = "Error: UID no encontrado"
                                    }
                                } else {
                                    registrationError = task.exception?.message ?: "Error en el registro"
                                }
                            }
                    } else {
                        // En caso de que estemos en Preview o sin auth inicializada
                        onRegistrationSuccess()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1980E6)),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = "Registrarse",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = "Ya tienes cuenta? Inicia sesión",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111418)
            )
        }
    }
}

/**
 * Preview de la pantalla de Registro.
 * Debe ser top-level para que Android Studio la muestre correctamente.
 */
@Preview(showBackground = true)
@Composable
fun RegistrationScreenPreview() {
    JDLW1Theme {
        RegistrationScreen()
    }
}
