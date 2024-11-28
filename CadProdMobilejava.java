import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

public class MainActivity extends AppCompatActivity {

    private EditText editTextProductName, editTextProductPrice;
    private Button buttonAddProduct;
    private ListView listViewProducts;

    private ArrayList<Product> productList;
    private ArrayAdapter<Product> productAdapter;

    private int selectedItemIndex = -1;
    private boolean isEditing = false;

    private static final String BASE_URL = "http://sua_api.com/";
    private ProductService productService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private static class Product {
        @SerializedName("name")
        String productName;
        @SerializedName("price")
        String productPrice;

        Product(String productName, String productPrice) {
            this.productName = productName;
            this.productPrice = productPrice;
        }

        @Override
        public String toString() {
            return productName + " - R$ " + productPrice;
        }
    }

    private interface ProductService {
        @GET("product/{productName}")
        Call<Product> getProduct(@Path("productName") String productName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextProductName = findViewById(R.id.editTextProductName);
        editTextProductPrice = findViewById(R.id.editTextProductPrice);
        buttonAddProduct = findViewById(R.id.buttonAddProduct);
        listViewProducts = findViewById(R.id.listViewProducts);

        productList = new ArrayList<>();
        productAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, productList);
        listViewProducts.setAdapter(productAdapter);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        productService = retrofit.create(ProductService.class);

        listViewProducts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                fetchProduct(position);
            }
        });

        buttonAddProduct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isEditing) {
                    saveEditedProduct();
                } else {
                    addProduct();
                }
            }
        });
    }

    private void addProduct() {
        String productName = editTextProductName.getText().toString().trim();
        String productPrice = editTextProductPrice.getText().toString().trim();

        if (!productName.isEmpty() && !productPrice.isEmpty()) {
            Product newProduct = new Product(productName, productPrice);
            productList.add(newProduct);
            productAdapter.notifyDataSetChanged();

            editTextProductName.setText("");
            editTextProductPrice.setText("");
        } else {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
        }
    }

    private void editProduct(int position) {
        Product product = productList.get(position);

        editTextProductName.setText(product.productName);
        editTextProductPrice.setText(product.productPrice);

        selectedItemIndex = position;
        isEditing = true;

        buttonAddProduct.setText("Salvar Edição");
    }

    private void saveEditedProduct() {
        String productName = editTextProductName.getText().toString().trim();
        String productPrice = editTextProductPrice.getText().toString().trim();

        if (!productName.isEmpty() && !productPrice.isEmpty()) {
            Product editedProduct = new Product(productName, productPrice);

            productList.set(selectedItemIndex, editedProduct);
            productAdapter.notifyDataSetChanged();

            editTextProductName.setText("");
            editTextProductPrice.setText("");
            selectedItemIndex = -1;
            isEditing = false;

            buttonAddProduct.setText("Adicionar Produto");
        } else {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchProduct(int position) {
        Product product = productList.get(position);

        executorService.execute(() -> {
            Call<Product> call = productService.getProduct(product.productName);

            call.enqueue(new Callback<Product>() {
                @Override
                public void onResponse(Call<Product> call, Response<Product> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        runOnUiThread(() -> editProduct(position));
                    } else {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro ao recuperar dados do produto", Toast.LENGTH_SHORT).show());
                    }
                }

                @Override
                public void onFailure(Call<Product> call, Throwable t) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Erro de conexão", Toast.LENGTH_SHORT).show());
                }
            });
        });
    }
}
