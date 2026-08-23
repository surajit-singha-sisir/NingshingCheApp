package com.example.data.ai

import com.example.data.model.AiChatMessage
import com.example.data.model.ArticleCitation
import com.example.data.repository.ArticleRepository
import com.example.data.repository.NinghsingCheContentData
import kotlinx.coroutines.delay
import java.util.UUID

class NinghsingCheAiAssistant(private val repository: ArticleRepository) {

    suspend fun answerQuestion(userQuestion: String): AiChatMessage {
        // Small realistic processing delay
        delay(700)

        val q = userQuestion.lowercase().trim()
        val citations = mutableListOf<ArticleCitation>()
        val answerText: String

        when {
            q.contains("ভাষা আন্দোলন") || q.contains("সুদেষ্ণা") || q.contains("১৯৫৫") || q.contains("১৬ই মার্চ") || q.contains("১৬ মার্চ") -> {
                val art1 = NinghsingCheContentData.articles.find { it.id == "art-1" }
                val art7 = NinghsingCheContentData.articles.find { it.id == "art-7" }

                if (art1 != null) {
                    citations.add(
                        ArticleCitation(
                            articleId = art1.id,
                            title = art1.title,
                            author = art1.authorName,
                            category = art1.category,
                            snippet = "১৯৫৫ সালের ভাষা আন্দোলনের ঐতিহাসিক পটভূমি ও মাতৃভাষার অধিকার রক্ষা সংগ্রাম।"
                        )
                    )
                }
                if (art7 != null) {
                    citations.add(
                        ArticleCitation(
                            articleId = art7.id,
                            title = art7.title,
                            author = art7.authorName,
                            category = art7.category,
                            snippet = "১৯৯৬ সালের ১৬ই মার্চ বরাক উপত্যকার পাথারকান্দিতে প্রথম নারী ভাষা শহীদ সুদেষ্ণা সিংহের আত্মত্যাগ।"
                        )
                    )
                }

                answerText = """
                    বিষ্ণুপ্রিয়া মণিপুরি ভাষা আন্দোলন ছিল নিজস্ব অস্তিত্ব, সংস্কৃতি ও শিক্ষার অধিকার রক্ষার এক ঐতিহাসিক লড়াই।
                    
                    • সূচনা: ১৯৫৫ সালের দিকে এই আন্দোলন সংগঠিত হয় যাতে প্রাথমিক বিদ্যালয়ে মাতৃভাষায় শিক্ষাদানের সাংবিধানিক অধিকার প্রতিষ্ঠিত হয়।
                    • রক্তিম অধ্যায় ও শহীদ দিবস: ১৯৯৬ সালের ১৬ই মার্চ আসামের পাথারকান্দির কলকলিঘাটে মিছিলে পুলিশের গুলিতে শহীদ হন সুদেষ্ণা সিংহ। তিনিই বিষ্ণুপ্রিয়া মণিপুরি ভাষা আন্দোলনের প্রথম নারী শহীদ।
                    • অর্জন: এর ফলে প্রাথমিক ও বিশ্ববিদ্যালয়ে ভাষাটি অন্তর্ভুক্ত হয় এবং আসাম ও ত্রিপুরায় সরকারি স্বীকৃতি লাভ করে।
                """.trimIndent()
            }

            q.contains("ইঞ্চৌঘর") || q.contains("বাস্তু") || q.contains("স্থাপত্য") || q.contains("ঘর") || q.contains("বাড়ি") -> {
                val art3 = NinghsingCheContentData.articles.find { it.id == "art-3" }
                if (art3 != null) {
                    citations.add(
                        ArticleCitation(
                            articleId = art3.id,
                            title = art3.title,
                            author = art3.authorName,
                            category = art3.category,
                            snippet = "প্রকৃতির সাথে নিবিড় সম্পর্কের পরিচায়ক 'ইঞ্চৌঘর' ও মাটির তৈরি ভিটের বাস্তুসংস্থান ও নির্মাণ শৈলী।"
                        )
                    )
                }

                answerText = """
                    'ইঞ্চৌঘর' হলো বিষ্ণুপ্রিয়া মণিপুরি সমাজের সুপ্রাচীন ঐতিহ্যবাহী লোক-স্থাপত্য ও বাস্তুসংস্থানের প্রতীক।
                    
                    • পরিবেশবান্ধব গঠন: খড়, বাঁশ, বেত ও মাটির প্রলেপ দিয়ে তৈরি এই ঘর গ্রীষ্মে শীতল ও শীতে উষ্ণ থাকে।
                    • মণ্ডপ ও ঠাকুরঘর: ঘরের সম্মুখভাগে থাকে প্রশস্ত বারান্দা বা সামাজিক মণ্ডপ, যেখানে বিচার-সালিশ, কীর্তন ও অতিথি আপ্যায়ন হয়। উত্তর-পূর্ব কোণে বা নির্দিষ্ট পবিত্র স্থানে থাকে ঠাকুরঘর।
                    • স্থাপত্যিক দর্শন: এটি কেবল আবাসস্থল নয়, বরং প্রকৃতি ও সম্প্রদায়ের সাথে সম্প্রীতির নিদর্শন।
                """.trimIndent()
            }

            q.contains("মিংকৌ") || q.contains("গোত্র") || q.contains("নামপ্রথা") || q.contains("সালাই") -> {
                val art4 = NinghsingCheContentData.articles.find { it.id == "art-4" }
                if (art4 != null) {
                    citations.add(
                        ArticleCitation(
                            articleId = art4.id,
                            title = art4.title,
                            author = art4.authorName,
                            category = art4.category,
                            snippet = "বিষ্ণুপ্রিয়া মণিপুরি গোত্র, বংশনাম এবং আত্মপরিচয়ের প্রতীক 'মিংকৌ' ব্যবস্থার উৎপত্তি।"
                        )
                    )
                }

                answerText = """
                    'মিংকৌ' বলতে বিষ্ণুপ্রিয়া মণিপুরি সমাজের ঐতিহ্যবাহী বংশপদবী, গোত্র (সালাই) ও আত্মপরিচয়ের নামপদ্ধতিকে বোঝায়।
                    
                    • সামাজিক বিন্যাস: প্রাচীনকালে বৃত্তিমূলক দায়িত্ব, ভৌগোলিক অবস্থান এবং সামাজিক অবদানের ওপর ভিত্তি করে এই নাম নির্ধারিত হতো।
                    • বৈবাহিক নিয়ন্ত্রণ: বিষ্ণুপ্রিয়া মণিপুরি সমাজে স্বগোত্রে বিবাহ নিয়ন্ত্রিত হওয়ায় মিংকৌ বংশলতিকা এবং রক্তীয় সম্পর্ক শনাক্ত করতে গুরুত্বপূর্ণ ভূমিকা রাখে।
                """.trimIndent()
            }

            q.contains("বিশু") || q.contains("নববর্ষ") || q.contains("উৎসব") || q.contains("হোলি") -> {
                val art6 = NinghsingCheContentData.articles.find { it.id == "art-6" }
                val art2 = NinghsingCheContentData.articles.find { it.id == "art-2" }
                if (art6 != null) {
                    citations.add(
                        ArticleCitation(
                            articleId = art6.id,
                            title = art6.title,
                            author = art6.authorName,
                            category = art6.category,
                            snippet = "চৈত্র সংক্রান্তি ও বৈশাখে উদযাপিত মণিপুরি নববর্ষ 'বিশু' উৎসব ও ১০৮ শাকের ভেষজ ঐতিহ্য।"
                        )
                    )
                }
                if (art2 != null) {
                    citations.add(
                        ArticleCitation(
                            articleId = art2.id,
                            title = art2.title,
                            author = art2.authorName,
                            category = art2.category,
                            snippet = "হোলি ও দোল পূর্ণিমার সময় পরিবেশিত থাবল চোংবা ও দলীয় লোকসংগীত।"
                        )
                    )
                }

                answerText = """
                    'বিশু' হলো বিষ্ণুপ্রিয়া মণিপুরিদের অন্যতম প্রধান প্রাকৃতিক ও সাংস্কৃতিক লোক-উৎসব, যা চৈত্র সংক্রান্তি ও বৈশাখ নববর্ষের সন্ধিক্ষণে পালিত হয়।
                    
                    • তিনটি পর্ব: কাঞ্জি বিশু (শাকসবজি ও প্রকৃতিবরণ), বাজে বিশু (মূল আনন্দ ও গুরুজনদের প্রণতি) এবং রং বিশু।
                    • ১০৮ শাক ও হেইরূই: বিশুর দিনে ১০৮ পদের শাক ও ঔষধি লতাপাতা সহযোগে প্রস্তুত বিশেষ খাদ্য শারীরিক রোগ প্রতিরোধ ক্ষমতা বৃদ্ধির প্রাচীন আয়ুর্বেদিক ঐতিহ্য।
                """.trimIndent()
            }

            q.contains("সাহিত্য") || q.contains("কবি") || q.contains("সেনাপতি রাজকুমার") || q.contains("গীতিস্বামী") -> {
                val art5 = NinghsingCheContentData.articles.find { it.id == "art-5" }
                val art2 = NinghsingCheContentData.articles.find { it.id == "art-2" }
                if (art5 != null) {
                    citations.add(
                        ArticleCitation(
                            articleId = art5.id,
                            title = art5.title,
                            author = art5.authorName,
                            category = art5.category,
                            snippet = "বিষ্ণুপ্রিয়া মণিপুরি সাহিত্যের আদিযুগ, মধ্যযুগের পদাবলী থেকে সেনাপতি রাজকুমারের আধুনিক পর্ব।"
                        )
                    )
                }
                if (art2 != null) {
                    citations.add(
                        ArticleCitation(
                            articleId = art2.id,
                            title = art2.title,
                            author = art2.authorName,
                            category = art2.category,
                            snippet = "গোকুলানন্দ গীতিস্বামীর সংগীত সাধনা ও সমাজ সংস্কারের পদাবলী।"
                        )
                    )
                }

                answerText = """
                    বিষ্ণুপ্রিয়া মণিপুরি সাহিত্যের ধারা প্রাচীন লোকগাথা, বৈষ্ণব পদাবলী এবং আধুনিক নবজাগরণে সমৃদ্ধ।
                    
                    • মধ্যযুগ ও পদাবলী: মধ্যযুগে ভাগবত ও রামায়ণের অনুবাদ এবং বৈষ্ণব ভক্তিধারার প্রকাশ ঘটে।
                    • আধুনিক সাহিত্যের রূপকার: বিংশ শতাব্দীর সূচনালগ্নে কবি সেনাপতি রাজকুমার, ব্রজেন্দ্র কুমার সিংহ এবং গোকুলানন্দ গীতিস্বামী আধুনিক সাহিত্যের ভিত রচনা করেন।
                    • নিংশিং চে তথ্যকোষের ভূমিকা: বর্তমান ডিজিটাল যুগে এই সাহিত্যধারাকে বিশ্বের সামনে উন্মুক্ত করছে নিংশিং চে।
                """.trimIndent()
            }

            q.contains("লোকতাক") || q.contains("হ্রদ") || q.contains("পৌরাণিক") || q.contains("ময়রাং") -> {
                val art8 = NinghsingCheContentData.articles.find { it.id == "art-8" }
                if (art8 != null) {
                    citations.add(
                        ArticleCitation(
                            articleId = art8.id,
                            title = art8.title,
                            author = art8.authorName,
                            category = art8.category,
                            snippet = "বিশ্বের একমাত্র ভাসমান হ্রদ লোকতাককে ঘিরে গড়ে ওঠা পৌরাণিক লোকবিশ্বাস ও সৃষ্টিগাথা।"
                        )
                    )
                }

                answerText = """
                    মণিপুরের লোকতাক হ্রদ হলো বিশ্বের একমাত্র প্রাকৃতিক ভাসমান হ্রদ ('ফুংডি') এবং বিষ্ণুপ্রিয়া মণিপুরি সংস্কৃতির এক অমূল্য পৌরাণিক স্মারক।
                    
                    • সৃষ্টিগাথা: বিশ্বাস করা হয় সৃষ্টির আদিমুহূর্তে দেবতারা লোকতাক হ্রদের তীরে প্রথম পদার্পণ করেছিলেন।
                    • লোকগাথা ও প্রেমোপাখ্যান: খাম্বা-থোইবীর মহাকাব্যিক প্রেম কাহিনী এবং ময়রাং কাংলেইপাকের বীরত্বগাথা এই হ্রদের তীরেই রচিত হয়েছিল।
                """.trimIndent()
            }

            else -> {
                // General query fallback searching the repository
                val matchedArticles = NinghsingCheContentData.articles.filter { article ->
                    article.title.contains(q, ignoreCase = true) ||
                            article.excerpt.contains(q, ignoreCase = true) ||
                            article.tags.any { tag -> tag.contains(q, ignoreCase = true) }
                }.take(2)

                if (matchedArticles.isNotEmpty()) {
                    matchedArticles.forEach { art ->
                        citations.add(
                            ArticleCitation(
                                articleId = art.id,
                                title = art.title,
                                author = art.authorName,
                                category = art.category,
                                snippet = art.excerpt
                            )
                        )
                    }
                    answerText = """
                        নিংশিং চে তথ্যকোষের আলোকে আপনার জিজ্ঞাসিত বিষয়ে প্রাসঙ্গিক তথ্যসমূহ সংগৃহীত হয়েছে। বিষ্ণুপ্রিয়া মণিপুরি ইতিহাস, সংস্কৃতি ও সাহিত্য সম্পর্কিত বিস্তৃত বিবরণের জন্য নিচের উল্লেখিত মূল প্রবন্ধগুলো পড়তে পারেন।
                    """.trimIndent()
                } else {
                    val defaultArt = NinghsingCheContentData.articles.first()
                    citations.add(
                        ArticleCitation(
                            articleId = defaultArt.id,
                            title = defaultArt.title,
                            author = defaultArt.authorName,
                            category = defaultArt.category,
                            snippet = defaultArt.excerpt
                        )
                    )
                    answerText = """
                        'নিংশিং চে — বিষ্ণুপ্রিয়া মণিপুরি তথ্যকোষ' হলো ভাষা, ইতিহাস, সমাজ ও সাহিত্য সংরক্ষণের একটি প্রামাণ্য ডিজিটাল আর্কাইভ।
                        
                        আপনি বিষ্ণুপ্রিয়া মণিপুরি ভাষা আন্দোলন, শহীদ সুদেষ্ণা সিংহ, ঐতিহ্যবাহী ইঞ্চৌঘর, মিংকৌ নামপ্রথা, বিশু উৎসব কিংবা সাহিত্যধারা সম্পর্কে প্রশ্ন করতে পারেন।
                    """.trimIndent()
                }
            }
        }

        return AiChatMessage(
            id = UUID.randomUUID().toString(),
            text = answerText,
            isUser = false,
            timestamp = System.currentTimeMillis(),
            citations = citations
        )
    }
}
