<div id="broadcast_message_container">
    <link rel="stylesheet" href="${request.contextPath}/plugin/org.joget.marketplace.BroadcastMessagePlugin/broadcastMessage.css">
    <script src="${request.contextPath}/plugin/org.joget.marketplace.BroadcastMessagePlugin/broadcastMessage.js"></script>

    <div class="broadcast-message-banner" id="broadcastBanner">
        <div class="broadcast-message-content">
            <!-- Pagination info - hidden but needed for JavaScript -->
            <span class="pagination-info" id="paginationInfo" style="display: none;"><span id="currentPage">1</span>/<span id="totalPages">${messageCount!2}</span></span>
             
            <div class="broadcast-icon">
                <i class="fas fa-bullhorn"></i>
            </div>
            <div class="broadcast-message-wrapper">
                <!-- Message content will be dynamically populated -->
                <div class="broadcast-text" id="broadcastText">
                    ${message!}
                </div>
            </div>
            <div class="broadcast-close" id="broadcastClose">
                <button class="mark-as-read-btn">Mark as Read</button>
            </div>

            <!-- Pagination buttons side by side with table layout for guaranteed horizontal alignment -->
            <table style="display: inline-table; margin-left: 5px; border-collapse: collapse; border: none;">
                <tr>
                    <td style="padding: 0;">
                        <!-- Left arrow button -->
                        <button class="pagination-btn" id="prevPage" title="Previous Message" style="display: ${(messageCount > 1)?string('inline-block', 'none')}; vertical-align: middle;">&laquo;</button>
                    </td>
                    <td style="padding: 0;">
                        <!-- Right arrow button -->
                        <button class="pagination-btn" id="nextPage" title="Next Message" style="display: ${(messageCount > 1)?string('inline-block', 'none')}; vertical-align: middle;">&raquo;</button>
                    </td>
                </tr>
            </table>
        </div>
    </div>
    
    <script>
        $(document).ready(function(){
            $("#broadcast_message_container").broadcastMessage({
                "contextPath" : "${request.contextPath}",
                "initialMessage" : "${message!}",
                "messagesData" : ${messagesData!"{}"}
            });
        });
    </script>
</div>
