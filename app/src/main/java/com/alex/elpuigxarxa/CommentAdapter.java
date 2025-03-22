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

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
    private List<Comment> listaComentarios = new ArrayList<>();
    private OnReplyClickListener replyClickListener;

    public interface OnReplyClickListener {
        void onReplyClick(String parentCommentId);
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

        holder.replyButton.setOnClickListener(v -> {
            replyClickListener.onReplyClick(comentario.id);
        });
    }

    @Override
    public int getItemCount() {
        return listaComentarios.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorTextView, contentTextView;
        Button replyButton;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            replyButton = itemView.findViewById(R.id.replyButton);
        }
    }
}
