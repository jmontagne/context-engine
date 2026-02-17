/**
 * Context Engine — Cross-Model Context Compaction for Multi-Turn AI Systems.
 *
 * <p>A <b>Context Engineering</b> POC that demonstrates a two-stage inference pipeline:
 * a cheap model (Gemini 2.0 Flash) summarizes conversation history before sending it
 * to an expensive model (Gemini 2.5 Pro). Achieves <b>~55% context window reduction</b>
 * with negligible quality loss.</p>
 *
 * <h2>The Problem</h2>
 * <p>In multi-turn agentic systems, conversation history grows linearly with each turn.
 * Sending full history to an expensive model wastes tokens and increases latency.
 * Context compaction solves this by intelligently summarizing prior turns.</p>
 *
 * <h2>Key Findings (Measured)</h2>
 * <table>
 *   <tr><th>Metric</th><th>Value</th></tr>
 *   <tr><td>Context reduction</td><td>~55% (272 tokens → 121 tokens on 8-message history)</td></tr>
 *   <tr><td>Compactor cost</td><td>Negligible (Gemini 2.0 Flash: ~$0.01/1M tokens)</td></tr>
 *   <tr><td>Quality impact</td><td>Negligible (key facts, decisions, preferences preserved)</td></tr>
 * </table>
 *
 * <h2>Architecture</h2>
 * <ul>
 *   <li><b>Compactor model:</b> Gemini 2.0 Flash — fast, cheap, summarizes history
 *       ({@link dev.jacques.contextengine.service.CompactorService})</li>
 *   <li><b>Inference model:</b> Gemini 2.5 Pro — high quality, answers using compacted context
 *       ({@link dev.jacques.contextengine.service.ChatService})</li>
 *   <li><b>A/B endpoints:</b> {@code /api/chat} (with compaction) vs {@code /api/chat-raw}
 *       (without) for direct cost comparison</li>
 * </ul>
 *
 * <h2>Tech Stack</h2>
 * <ul>
 *   <li>Java 21, Spring Boot 3.4, LangChain4j 1.0.0-beta1</li>
 *   <li>Google Vertex AI: Gemini 2.5 Pro + Gemini 2.0 Flash</li>
 *   <li>GCP Cloud Run (scale-to-zero, 512Mi, us-central1)</li>
 *   <li>Terraform IaC (Cloud Run, Artifact Registry, IAM, API enablement)</li>
 * </ul>
 *
 * <h2>Key Insight</h2>
 * <p><i>Managing what enters the context window matters more than prompt phrasing —
 * especially in multi-turn agentic systems.</i></p>
 *
 * @see dev.jacques.contextengine.service.CompactorService Context compaction logic
 * @see dev.jacques.contextengine.service.ChatService Two-stage inference orchestration
 */
package dev.jacques.contextengine;
