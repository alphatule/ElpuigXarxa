package com.alex.elpuigxarxa;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.appwrite.exceptions.AppwriteException;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private List<Comment> listaComentarios = new ArrayList<>();
    private OnReplyClickListener replyClickListener;

    public interface OnReplyClickListener {
        void onReplyClick(String postId, String parentCommentId, String content) throws AppwriteException;
    }

    public CommentAdapter(OnReplyClickListener replyClickListener) {
        this.replyClickListener = replyClickListener;
    }

    public void establecerLista(List<Comment> comentarios) {
        this.listaComentarios = comentarios;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.viewholder_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comentario = listaComentarios.get(position);
        holder.authorTextView.setText(comentario.author);
        holder.contentTextView.setText(comentario.content);

        String formattedDate = formatTimestamp(comentario.timestamp);
        holder.timestampTextView.setText(formattedDate);

        // Muestra el campo para escribir respuesta al presionar "Responder"
        holder.replyButton.setOnClickListener(v -> {
            holder.replyEditText.setVisibility(View.VISIBLE);
            holder.sendReplyButton.setVisibility(View.VISIBLE);
        });

        // Enviar respuesta
        holder.sendReplyButton.setOnClickListener(v -> {
            String respuesta = holder.replyEditText.getText().toString().trim();
            if (!respuesta.isEmpty() && replyClickListener != null) {
                try {
                    replyClickListener.onReplyClick(comentario.postId, comentario.id, respuesta);
                } catch (AppwriteException e) {
                    throw new RuntimeException(e);
                }
                holder.replyEditText.setText("");
                holder.replyEditText.setVisibility(View.GONE);
                holder.sendReplyButton.setVisibility(View.GONE);
            }
        });

        // Cargar respuestas si existen
        if (comentario.replies != null && !comentario.replies.isEmpty()) {
            holder.repliesRecyclerView.setVisibility(View.VISIBLE);
            CommentAdapter repliesAdapter = new CommentAdapter(replyClickListener);
            holder.repliesRecyclerView.setAdapter(repliesAdapter);
            repliesAdapter.establecerLista(comentario.replies);
        } else {
            holder.repliesRecyclerView.setVisibility(View.GONE);
        }
    }

    // Método para formatear la fecha
    private String formatTimestamp(String timestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd 'de' MMM 'a las' HH:mm", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            return outputFormat.format(date);
        } catch (Exception e) {
            return "Fecha desconocida"; // Si el timestamp no es válido
        }
    }

    @Override
    public int getItemCount() {
        return listaComentarios.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView, contentTextView;
        EditText replyEditText;
        Button replyButton, sendReplyButton;
        RecyclerView repliesRecyclerView;
        TextView timestampTextView;


        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            replyEditText = itemView.findViewById(R.id.replyEditText);
            replyButton = itemView.findViewById(R.id.replyButton);
            sendReplyButton = itemView.findViewById(R.id.sendReplyButton);
            repliesRecyclerView = itemView.findViewById(R.id.repliesRecyclerView);
            repliesRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            timestampTextView = itemView.findViewById(R.id.timestampTextView);
        }
    }
}
