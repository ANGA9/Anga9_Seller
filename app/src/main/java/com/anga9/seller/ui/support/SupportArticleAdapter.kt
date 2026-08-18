package com.anga9.seller.ui.support

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.anga9.seller.R
import com.anga9.seller.data.model.support.SupportArticle

/**
 * Shared adapter for article lists in Seller App.
 * Used in HelpSupportActivity (popular articles) and HelpArticlesActivity (filtered list).
 */
class SupportArticleAdapter(
    private val onClick: (SupportArticle) -> Unit
) : ListAdapter<SupportArticle, SupportArticleAdapter.ArticleVH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_support_article, parent, false)
        return ArticleVH(view)
    }

    override fun onBindViewHolder(holder: ArticleVH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ArticleVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView    = view.findViewById(R.id.tvArticleTitle)
        private val tvCategory: TextView = view.findViewById(R.id.tvArticleCategory)

        fun bind(article: SupportArticle) {
            tvTitle.text    = article.title
            tvCategory.text = article.category.replaceFirstChar { it.uppercase() }
            itemView.setOnClickListener { onClick(article) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SupportArticle>() {
            override fun areItemsTheSame(old: SupportArticle, new: SupportArticle) = old.slug == new.slug
            override fun areContentsTheSame(old: SupportArticle, new: SupportArticle) = old == new
        }
    }
}