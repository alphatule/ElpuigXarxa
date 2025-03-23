package com.alex.elpuigxarxa;

import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.appwrite.Client;
import io.appwrite.Query;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;
import io.appwrite.models.Document;
import io.appwrite.models.DocumentList;
import io.appwrite.services.Account;
import io.appwrite.services.Databases;

public class HomeFragment extends Fragment {
    NavController navController;

    ImageView photoImageView;
    TextView displayNameTextView, emailTextView;

    Client client;
    Account account;
    String userId;
    PostsAdapter adapter;
    AppViewModel appViewModel;

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        NavigationView navigationView = view.getRootView().findViewById(R.id.nav_view);
        View header = navigationView.getHeaderView(0);
        photoImageView = header.findViewById(R.id.imageView);
        displayNameTextView = header.findViewById(R.id.displayNameTextView);
        emailTextView = header.findViewById(R.id.emailTextView);
        client = new Client(requireContext()).setProject(getString(R.string.APPWRITE_PROJECT_ID));
        account = new Account(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            account.get(new CoroutineCallback<>((result, error) -> {
                if (error != null) {
                    error.printStackTrace();
                    return;
                }
                mainHandler.post(() -> {
                    userId = result.getId();
                    displayNameTextView.setText(result.getName().toString());
                    emailTextView.setText(result.getEmail().toString());
                    Glide.with(requireView()).load(R.drawable.user).into(photoImageView);
                    obtenerPosts();
                });
            }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
        view.findViewById(R.id.gotoNewPostFragmentButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navController.navigate(R.id.newPostFragment);
            }
        });
        RecyclerView postsRecyclerView = view.findViewById(R.id.postsRecyclerView);
        adapter = new PostsAdapter();
        postsRecyclerView.setAdapter(adapter);

        appViewModel = new ViewModelProvider(requireActivity()).get(AppViewModel.class);
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView authorPhotoImageView, likeImageView, mediaImageView, deletePostImageView;
        TextView authorTextView, contentTextView, numLikesTextView;
        EditText commentEditText;
        Button commentButton;
        RecyclerView commentsRecyclerView;
        CommentAdapter commentAdapter;

        @RequiresApi(api = Build.VERSION_CODES.O)
        PostViewHolder(@NonNull View itemView) {
            super(itemView);
            authorPhotoImageView = itemView.findViewById(R.id.authorPhotoImageView);
            likeImageView = itemView.findViewById(R.id.likeImageView);
            mediaImageView = itemView.findViewById(R.id.mediaImage);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            numLikesTextView = itemView.findViewById(R.id.numLikesTextView);
            deletePostImageView = itemView.findViewById(R.id.deletePostImageView);

            commentEditText = itemView.findViewById(R.id.commentEditText);
            commentButton = itemView.findViewById(R.id.commentButton);
            commentsRecyclerView = itemView.findViewById(R.id.commentsRecyclerView);
            commentsRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));

            commentAdapter = new CommentAdapter((postId, parentCommentId, content) -> {
                guardarComentario(postId, parentCommentId, content);
            });
            commentsRecyclerView.setAdapter(commentAdapter);
        }
    }


    class PostsAdapter extends RecyclerView.Adapter<PostViewHolder> {
        DocumentList<Map<String, Object>> lista = null;

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return new PostViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_post, parent, false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            Map<String, Object> post = lista.getDocuments().get(position).getData();

            // Cargar imagen de perfil del autor
            if (post.get("authorPhotoUrl") == null) {
                holder.authorPhotoImageView.setImageResource(R.drawable.user);
            } else {
                Glide.with(getContext()).load(post.get("authorPhotoUrl").toString()).circleCrop().into(holder.authorPhotoImageView);
            }

            // Mostrar autor y contenido
            holder.authorTextView.setText(post.get("author").toString());
            holder.contentTextView.setText(post.get("content").toString());

            // Mostrar o esconder botón de eliminar según el usuario logueado
            String postUserId = post.get("uid").toString();
            if (postUserId.equals(userId)) {
                holder.deletePostImageView.setVisibility(View.VISIBLE);
            } else {
                holder.deletePostImageView.setVisibility(View.GONE);
            }

            holder.deletePostImageView.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar Post")
                        .setMessage("¿Seguro que deseas eliminar este post?")
                        .setPositiveButton("Eliminar", (dialog, which) -> eliminarPost(post.get("$id").toString()))
                        .setNegativeButton("Cancelar", null)
                        .show();
            });

            // Gestión de likes
            List<String> likes = (List<String>) post.get("likes");
            if (likes.contains(userId)) holder.likeImageView.setImageResource(R.drawable.like_on);
            else holder.likeImageView.setImageResource(R.drawable.like_off);

            holder.numLikesTextView.setText(String.valueOf(likes.size()));

            holder.likeImageView.setOnClickListener(view -> {
                Databases databases = new Databases(client);
                Handler mainHandler = new Handler(Looper.getMainLooper());
                List<String> nuevosLikes = new ArrayList<>(likes);
                if (nuevosLikes.contains(userId)) nuevosLikes.remove(userId);
                else nuevosLikes.add(userId);

                Map<String, Object> data = new HashMap<>();
                data.put("likes", nuevosLikes);

                try {
                    databases.updateDocument(
                            getString(R.string.APPWRITE_DATABASE_ID),
                            getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                            post.get("$id").toString(),
                            data,
                            new ArrayList<>(),
                            new CoroutineCallback<>((result, error) -> {
                                if (error != null) {
                                    error.printStackTrace();
                                    return;
                                }
                                System.out.println("Likes actualizados:" + result.toString());
                                mainHandler.post(() -> obtenerPosts());
                            })
                    );
                } catch (AppwriteException e) {
                    throw new RuntimeException(e);
                }
            });

            // Miniatura de media
            if (post.get("mediaUrl") != null) {
                holder.mediaImageView.setVisibility(View.VISIBLE);
                if ("audio".equals(post.get("mediaType").toString())) {
                    Glide.with(requireView()).load(R.drawable.audio).centerCrop().into(holder.mediaImageView);
                } else {
                    Glide.with(requireView()).load(post.get("mediaUrl").toString()).centerCrop().into(holder.mediaImageView);
                }
                holder.mediaImageView.setOnClickListener(view -> {
                    appViewModel.postSeleccionado.setValue(post);
                    navController.navigate(R.id.mediaFragment);
                });
            } else {
                holder.mediaImageView.setVisibility(View.GONE);
            }

            // **Nuevo: Cargar comentarios**
            try {
                cargarComentarios(post.get("$id").toString(), holder);
            } catch (AppwriteException e) {
                throw new RuntimeException(e);
            }

            // **Nuevo: Manejo de comentarios**
            holder.commentButton.setOnClickListener(v -> {
                String commentText = holder.commentEditText.getText().toString().trim();
                if (!commentText.isEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            guardarComentario(post.get("$id").toString(), null, commentText);
                        } catch (AppwriteException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    holder.commentEditText.setText(""); // Limpiar campo después de comentar
                }
            });
        }


        @Override
        public int getItemCount() {
            return lista == null ? 0 : lista.getDocuments().size();
        }

        public void establecerLista(DocumentList<Map<String, Object>> lista) {
            this.lista = lista;
            notifyDataSetChanged();
        }
    }

    void eliminarPost(String postId) {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        databases.deleteDocument(
                getString(R.string.APPWRITE_DATABASE_ID),
                getString(R.string.APPWRITE_POSTS_COLLECTION_ID),
                postId,
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        Snackbar.make(requireView(), "Error eliminando post: " + error.getMessage(), Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    mainHandler.post(() -> {
                        Snackbar.make(requireView(), "Post eliminado correctamente", Snackbar.LENGTH_SHORT).show();
                        obtenerPosts(); // Refrescar la lista
                    });
                })
        );
    }


    void obtenerPosts() {
        Databases databases = new Databases(client);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        try {
            databases.listDocuments(getString(R.string.APPWRITE_DATABASE_ID), // databaseId
                    getString(R.string.APPWRITE_POSTS_COLLECTION_ID), // collectionId
                    new ArrayList<>(), // queries (optional)
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Snackbar.make(requireView(), "Error al obtener los posts: " + error.toString(), Snackbar.LENGTH_LONG).show();
                            return;
                        }
                        System.out.println(result.toString());
                        mainHandler.post(() -> adapter.establecerLista(result));
                    }));
        } catch (AppwriteException e) {
            throw new RuntimeException(e);
        }
    }

     // Comentarios

    @RequiresApi(api = Build.VERSION_CODES.O)
    void guardarComentario(String postId, String parentCommentId, String content) throws AppwriteException {
        Databases databases = new Databases(client);
        Map<String, Object> data = new HashMap<>();
        data.put("postId", postId);
        data.put("parentCommentId", parentCommentId); // Si es una respuesta, guarda el ID del comentario padre
        data.put("author", displayNameTextView.getText().toString());
        data.put("authorPhotoUrl", null); // Puedes cambiarlo si tienes URL de la foto
        data.put("uid", userId);
        data.put("content", content);
        data.put("timestamp", Instant.now().toString());

        databases.createDocument(
                getString(R.string.APPWRITE_DATABASE_ID),
                getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                "unique()",
                data,
                new ArrayList<>(),
                new CoroutineCallback<>((result, error) -> {
                    if (error != null) {
                        Snackbar.make(requireView(), "Error al publicar comentario", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    Snackbar.make(requireView(), "Comentario publicado", Snackbar.LENGTH_SHORT).show();
//                    cargarComentarios(postId); // Refrescar comentarios después de responder

                    requireActivity().runOnUiThread(this::obtenerPosts); // Refrescar todos los posts
                })
        );
    }


    void cargarComentarios(String postId, PostViewHolder holder) throws AppwriteException {
        Databases databases = new Databases(client);
        List<String> queries = new ArrayList<>();
        queries.add(Query.Companion.equal("postId", List.of(postId)));

        databases.listDocuments(
                getString(R.string.APPWRITE_DATABASE_ID),
                getString(R.string.APPWRITE_COMMENTS_COLLECTION_ID),
                queries,
                new CoroutineCallback<DocumentList<Map<String, Object>>>((result, error) -> {
                    if (error != null) {
                        Snackbar.make(requireView(), "Error al obtener comentarios: " + error.getMessage(), Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    List<Comment> listaComentarios = new ArrayList<>();
                    Map<String, Comment> commentsMap = new HashMap<>();

                    try {
                        // Primero, creamos los comentarios sin anidar
                        for (var doc : result.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            Comment comment = new Comment(
                                    doc.getId(),
                                    data.get("postId").toString(),
                                    data.get("parentCommentId") != null ? data.get("parentCommentId").toString() : null,
                                    data.get("author").toString(),
                                    data.get("authorPhotoUrl") != null ? data.get("authorPhotoUrl").toString() : null,
                                    data.get("uid").toString(),
                                    data.get("content").toString(),
                                    data.get("timestamp").toString()
                            );
                            commentsMap.put(comment.id, comment);
                        }

                        // Luego, estructuramos los comentarios anidados
                        for (Comment comment : commentsMap.values()) {
                            if (comment.parentCommentId == null) {
                                listaComentarios.add(comment);
                            } else {
                                Comment parentComment = commentsMap.get(comment.parentCommentId);
                                if (parentComment != null) {
                                    parentComment.replies.add(comment);
                                }
                            }
                        }

                    } catch (Exception e) {
                        Snackbar.make(requireView(), "Error procesando comentarios: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }

                    requireActivity().runOnUiThread(() -> {
                        holder.commentAdapter.establecerLista(listaComentarios);
                    });
                })
        );
    }


}