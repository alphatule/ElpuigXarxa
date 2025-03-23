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
                userId = result.getId();
                displayNameTextView.setText(result.getName());
                emailTextView.setText(result.getEmail());

                // Obtener imagen de perfil desde Auth
                String profileImageUrl = result.getPrefs().getData().containsKey("profileImage")
                        ? result.getPrefs().getData().get("profileImage").toString()
                        : null;

                if (profileImageUrl != null) {
                    Glide.with(requireView()).load(profileImageUrl).into(photoImageView);
                } else {
                    photoImageView.setImageResource(R.drawable.user);
                }
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
        File imageFile = new File(selectedImageUri.getPath());

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

                    try {
                        updateProfileImage(imageUrl);
                    } catch (AppwriteException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }

    // **Actualizar imagen de perfil en Auth**
    private void updateProfileImage(String imageUrl) throws AppwriteException {
        account.updatePrefs(
                new HashMap<String, Object>() {{
                    put("profileImage", imageUrl);
                }},
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        error.printStackTrace();
                        return;
                    }
                    requireActivity().runOnUiThread(() -> Glide.with(requireView()).load(imageUrl).into(photoImageView));
                    try {
                        updatePostsImage(imageUrl);
                    } catch (AppwriteException e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }


    // Actualizar la imagen de perfil en los posts
    private void updatePostsImage(String imageUrl) throws AppwriteException {
        Databases databases = new Databases(client);
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("uid", List.of(userId))); // Encuentra los posts del usuario

        databases.listDocuments(
                getString(R.string.APPWRITE_DATABASE_ID),
                getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                queries,
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        error.printStackTrace();
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
                                            updateError.printStackTrace();
                                        }
                                    })
                            );
                        } catch (AppwriteException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
        );
    }

}
