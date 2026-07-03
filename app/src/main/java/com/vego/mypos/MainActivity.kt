// POS con flujo de pantallas usando Navigation Compose
package com.vego.mypos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import java.util.Objects.equals

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val navController = rememberNavController()
                AppNavHost(navController)
            }
        }
    }
}

// -------------------- MODELOS --------------------
data class Product(val name: String, val price: Double)

// -------------------- NAVEGACIÓN --------------------
sealed class Screen(val route: String) {
    object Products : Screen("products")
    object PaymentMethod : Screen("payment_method")
    object Auth : Screen("auth")
    object Processing : Screen("processing")
    object Result : Screen("result/{success}") {
        fun createRoute(success: Boolean) = "result/$success"
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    var cart by remember { mutableStateOf(listOf<Product>()) }

    NavHost(navController, startDestination = Screen.Products.route) {
        composable(Screen.Products.route) {
            ProductScreen(cart) { updatedCart ->
                cart = updatedCart
                navController.navigate(Screen.PaymentMethod.route)
            }
        }
        composable(Screen.PaymentMethod.route) {
            PaymentMethodScreen(cart.sumOf { it.price }) {
                navController.navigate(Screen.Auth.route)
            }
        }
        composable(Screen.Auth.route) {
            AuthScreen {
                navController.navigate(Screen.Processing.route)
            }
        }
        composable(Screen.Processing.route) {
            ProcessingScreen(navController)
        }
        composable(Screen.Result.route) { backStackEntry ->
            val success = backStackEntry.arguments?.getString("success").toBoolean()
            ResultScreen(success) {
                cart = emptyList()
                navController.navigate(Screen.Products.route) {
                    popUpTo(0)
                }
            }
        }
    }
}

// -------------------- PANTALLAS --------------------
@Composable
fun ProductScreen(cart: List<Product>, onCheckout: (List<Product>) -> Unit) {
    val products = listOf(
        Product("Café", 25.0),
        Product("Pan", 10.0),
        Product("Refresco", 18.0)
    )
    var currentCart by remember { mutableStateOf(cart) }

    Column(Modifier.padding(16.dp)) {
        Text("Productos", style = MaterialTheme.typography.headlineMedium)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(products) { product ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${product.name} $${product.price}")
                    Button(onClick = { currentCart = currentCart + product }) {
                        Text("Agregar")
                    }
                }
            }
        }
        Text("Total: $${currentCart.sumOf { it.price }}", fontWeight = FontWeight.Bold)
        Button(
            onClick = { onCheckout(currentCart) },
            enabled = currentCart.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuar a Pago")
        }
    }
}

@Composable
fun PaymentMethodScreen(total: Double, onSelect: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Total a pagar: $${total}", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onSelect, modifier = Modifier.fillMaxWidth(0.8f)) {
            Text("Pago NFC (Móvil)")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onSelect, modifier = Modifier.fillMaxWidth(0.8f)) {
            Text("Tarjeta Crédito / Débito")
        }
    }
}

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ingrese NIP o acerque dispositivo", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onAuthenticated) {
            Text("Confirmar")
        }
    }
}

@Composable
fun ProcessingScreen(navController: NavHostController) {
    LaunchedEffect(Unit) {
        delay(2000)
        val success = listOf(true, false).random()
        navController.navigate(Screen.Result.createRoute(success)) {
            popUpTo(Screen.Processing.route) { inclusive = true }
        }
    }
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Procesando pago...")
    }
}

@Composable
fun ResultScreen(success: Boolean, onFinish: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (success) "Pago exitoso" else "Pago rechazado\nFondos insuficientes",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onFinish) {
            Text("Nueva Venta")
        }
    }
}
