package com.alex.elpuigxarxa;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

import io.appwrite.exceptions.AppwriteException;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private List<Comment> listaComentarios = new ArrayList<>();
    private OnReplyClickListener replyClickListener;

    // Definir interfaz con tres parámetros
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

        // Mostrar campo para escribir la respuesta cuando el usuario toque "Responder"
        holder.replyButton.setOnClickListener(v -> {
            if (holder.replyEditText.getVisibility() == View.GONE) {
                holder.replyEditText.setVisibility(View.VISIBLE);
            } else {
                holder.replyEditText.setVisibility(View.GONE);
            }
        });

        // Enviar respuesta cuando el usuario escriba y presione el botón "Responder"
        holder.replyButton.setOnClickListener(v -> {
            String respuesta = holder.replyEditText.getText().toString().trim();
            if (!respuesta.isEmpty() && replyClickListener != null) {
                try {
                    replyClickListener.onReplyClick(comentario.postId, comentario.id, respuesta);
                } catch (AppwriteException e) {
                    throw new RuntimeException(e);
                }
                holder.replyEditText.setText("");  // Limpiar después de enviar
                holder.replyEditText.setVisibility(View.GONE);
            }
        });
    }


    @Override
    public int getItemCount() {
        return listaComentarios.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView, contentTextView;
        EditText replyEditText;
        Button replyButton;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            replyEditText = itemView.findViewById(R.id.replyEditText);
            replyButton = itemView.findViewById(R.id.replyButton);
        }
    }
}