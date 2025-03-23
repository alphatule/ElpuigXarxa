package com.alex.elpuigxarxa;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import io.appwrite.Client;
import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Document;
import io.appwrite.models.InputFile;
import io.appwrite.models.User;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;
import io.appwrite.services.Storage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {
    NavController navController;
    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;
    Button changePhotoButton;
    Uri selectedImageUri;
    Client client;
    Account account;
    Storage storage;
    Databases databases;
    String userId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);
        photoImageView = view.findViewById(R.id.photoImageView);
        displayNameTextView = view.findViewById(R.id.displayNameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        changePhotoButton = view.findViewById(R.id.changePhotoButton);

        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        storage = new Storage(client);
        databases = new Databases(client);

        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }

                System.out.println("DEBUG: Datos de usuario obtenidos de Auth → " + result.getPrefs().getData());

                String profileImageUrl = result.getPrefs().getData().containsKey("profileImage")
                        ? result.getPrefs().getData().get("profileImage").toString()
                        : null;

                requireActivity().runOnUiThread(() -> {
                    displayNameTextView.setText(result.getName());
                    emailTextView.setText(result.getEmail());

                    if (profileImageUrl != null) {
                        Glide.with(requireView()).load(profileImageUrl).into(photoImageView);
                    } else {
                        photoImageView.setImageResource(R.drawable.user);
                    }
                });
            }));

        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }

        // Botón para cambiar la foto de perfil
        changePhotoButton.setOnClickListener(v -> selectImageFromGallery());
    }

    // Seleccionar imagen desde la galería
    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    // Manejar el resultado de la selección de imagen
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            Glide.with(requireView()).load(selectedImageUri).into(photoImageView);
            uploadProfileImage();
        }
    }

    private void uploadProfileImage() {
        if (selectedImageUri == null) return;

        try {
            File imageFile = getFileFromUri(selectedImageUri);

            storage.createFile(
                    getString(R.string.APPWRITE_STORAGE_BUCKET_ID),
                    "unique()",
                    InputFile.Companion.fromFile(imageFile),
                    new ArrayList<>(),
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            error.printStackTrace();
                            return;
                        }

                        String imageUrl = "https://cloud.appwrite.io/v1/storage/buckets/" +
                                getString(R.string.APPWRITE_STORAGE_BUCKET_ID) +
                                "/files/" + result.getId() + "/view?project=" +
                                getString(R.string.APPWRITE_PROJECT_ID);

                        updateProfileImage(imageUrl);
                    })
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // **Actualizar imagen de perfil en Auth**
    private void updateProfileImage(String imageUrl) {
        System.out.println("DEBUG: Intentando actualizar imagen en Auth con URL → " + imageUrl);

        try {
            account.updatePrefs(
                    new HashMap<String, Object>() {{
                        put("profileImage", imageUrl);
                    }},
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            System.err.println("ERROR: No se pudo actualizar la imagen en Auth → " + error.getMessage());
                            return;
                        }

                        System.out.println("DEBUG: Imagen de perfil actualizada en Auth con éxito.");
                        requireActivity().runOnUiThread(() -> Glide.with(requireView()).load(imageUrl).into(photoImageView));

                        // Ahora actualizamos los posts del usuario
                        updatePostsImage(imageUrl);
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }


    private File getFileFromUri(Uri uri) throws IOException {
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new FileNotFoundException("No se pudo abrir el URI: " + uri);
        }

        String fileName = "temp_profile_image.jpg";
        File tempFile = new File(requireContext().getCacheDir(), fileName);

        FileOutputStream outputStream = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        outputStream.close();
        inputStream.close();
        return tempFile;
    }



    // Actualizar la imagen de perfil en los posts
    private void updatePostsImage(String imageUrl) {
        System.out.println("DEBUG: Intentando actualizar la imagen en los posts del usuario.");

        Databases databases = new Databases(client);
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("uid", List.of(userId))); // Encuentra los posts del usuario

        try {
            databases.listDocuments(
                    getString(R.string.APPWRITE_DATABASE_ID),
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                    queries,
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            System.err.println("ERROR: No se pudieron obtener los posts del usuario → " + error.getMessage());
                            return;
                        }

                        for (Document<Map<String, Object>> post : result.getDocuments()) {
                            Map<String, Object> updateData = new HashMap<>();
                            updateData.put("authorPhotoUrl", imageUrl);

                            try {
                                databases.updateDocument(
                                        getString(R.string.APPWRITE_DATABASE_ID),
                                        getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                                        post.getId(),
                                        updateData,
                                        new ArrayList<>(),
                                        new CoroutineCallback<>((updateResult, updateError) -> {
                                            if (updateError != null) {
                                                System.err.println("ERROR: No se pudo actualizar la imagen en un post → " + updateError.getMessage());
                                                return;
                                            }
                                            System.out.println("DEBUG: Imagen actualizada en el post con ID → " + post.getId());
                                        })
                                );
                            } catch (AppwriteException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    })
            );
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }


}
