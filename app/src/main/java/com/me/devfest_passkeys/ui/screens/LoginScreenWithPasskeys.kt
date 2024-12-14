package com.me.devfest_passkeys.ui.screens

import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.me.devfest_passkeys.R
import com.me.devfest_passkeys.ui.state.LoginEvent
import com.me.devfest_passkeys.ui.theme.TextColor
import com.me.devfest_passkeys.ui.viewModel.LoginWithPasskeysViewModel

@Composable
fun LoginScreenWithPasskeys(navController: NavController, activity: ComponentActivity) {
    val viewModel: LoginWithPasskeysViewModel by activity.viewModels<LoginWithPasskeysViewModel>()

    var emailError by remember { mutableStateOf(false) }

    val username = remember { mutableStateOf(TextFieldValue()) }

    LaunchedEffect(Unit) {
        viewModel.loginEvents.collect { event ->
            when (event) {
                is LoginEvent.NoCredentials -> {
                    Toast.makeText(
                        activity,
                        "Seems like you have not setup PassKeys for our application",
                        Toast.LENGTH_LONG
                    ).show()
                }

                is LoginEvent.Error -> {
                    Toast.makeText(activity, event.message, Toast.LENGTH_LONG).show()
                }

                is LoginEvent.Success -> {
                    navController.navigate("home/${event.email}")
                }

                is LoginEvent.EmailError -> {
                    emailError = true
                }

                else -> {}
            }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Login with Passkeys", style = MaterialTheme.typography.headlineMedium)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    label = { Text(text = "Email", color = TextColor) },
                    value = username.value,
                    onValueChange = { emailError = false
                        username.value = it },
                    isError = emailError,
                    supportingText = { if (emailError) Text(text = "Invalid input, please type a valid email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "emailIcon"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp)
                        .padding(horizontal = 12.dp)
                )

                Button(
                    onClick = {
                        viewModel.createPasswordCredential(
                            activity,
                            username.value.text,
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text("Login/Signup")
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Divider(modifier = Modifier.weight(1F))
                    Text(text = "OR", modifier = Modifier.padding(horizontal = 8.dp))
                    Divider(modifier = Modifier.weight(1F))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.retrieveCredentials(activity) },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Color(0xFF79747E)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextColor),
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_passkey),
                            contentDescription = "Passkey Icon",
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.CenterVertically)
                        )
                        Text("Sign in with a passkey", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenWithPasskeysPreview() {
    LoginScreenWithPasskeys(
        navController = rememberNavController(),
        activity = object : ComponentActivity() {} // Mock activity
    )
}