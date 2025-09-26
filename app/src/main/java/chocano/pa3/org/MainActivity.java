package chocano.pa3.org;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailField, passwordField;
    private Button registerButton, loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Oculta SOLO en Main
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        mAuth = FirebaseAuth.getInstance();

        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        registerButton = findViewById(R.id.register);
        loginButton = findViewById(R.id.login);

        registerButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString();
            if (!validate(email, password)) return;
            registerUser(email, password);
        });

        loginButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString();
            if (!validate(email, password)) return;
            loginUser(email, password);
        });
    }

    // 🔐 Si ya hay sesión, salta directo a Home
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        if (current != null) {
            // Si usas verificación de correo, puedes exigirla:
            // if (!current.isEmailVerified()) return;
            goToHome();
        }
    }

    private boolean validate(String email, String password) {
        boolean ok = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Correo inválido"); ok = false;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordField.setError("Mínimo 6 caracteres"); ok = false;
        }
        return ok;
    }

    private void registerUser(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                        goToHome();
                    } else {
                        Toast.makeText(this, "Registro fallido: "
                                        + (task.getException()!=null?task.getException().getMessage():""),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        goToHome();
                    } else {
                        Toast.makeText(this, "Inicio fallido: "
                                        + (task.getException()!=null?task.getException().getMessage():""),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void goToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish(); // evita volver a Main con "Atrás"
    }
}
