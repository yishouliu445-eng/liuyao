package com.yishou.liuyao.book.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.book.domain.Book;
import com.yishou.liuyao.book.dto.BookDTO;
import com.yishou.liuyao.book.dto.BookImportRequest;
import com.yishou.liuyao.book.dto.BookImportResponse;
import com.yishou.liuyao.book.mapper.BookMapper;
import com.yishou.liuyao.book.repository.BookRepository;
import com.yishou.liuyao.infrastructure.util.JsonUtils;
import com.yishou.liuyao.task.domain.DocProcessTask;
import com.yishou.liuyao.task.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);

    private final BookRepository bookRepository;
    private final TaskRepository taskRepository;
    private final BookMapper bookMapper;
    private final ObjectMapper objectMapper;

    public BookService(BookRepository bookRepository,
                       TaskRepository taskRepository,
                       BookMapper bookMapper,
                       ObjectMapper objectMapper) {
        this.bookRepository = bookRepository;
        this.taskRepository = taskRepository;
        this.bookMapper = bookMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<BookDTO> listRecentBooks() {
        return bookRepository.findTop20ByOrderByIdDesc().stream().map(bookMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Book getBook(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalStateException("书目不存在: " + bookId));
    }

    @Transactional
    public void updateParseStatus(Long bookId, String parseStatus) {
        Book book = getBook(bookId);
        book.setParseStatus(parseStatus);
        bookRepository.save(book);
    }

    @Transactional
    public BookImportResponse createImportRequest(BookImportRequest request) {
        // 资料导入第一步先只做“登记原始文件 + 建解析任务”，
        // 后续真正的切片、抽取和知识入库都沿着 task 链继续走。
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setSourceType(request.getSourceType());
        book.setFilePath(request.getFilePath());
        book.setFileSize(request.getFileSize());
        book.setParseStatus("PENDING");
        book.setRemark(request.getRemark());
        book = bookRepository.save(book);

        DocProcessTask task = new DocProcessTask();
        task.setTaskType("BOOK_PARSE");
        task.setRefId(book.getId());
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setPayloadJson(JsonUtils.toJson(objectMapper, request));
        task = taskRepository.save(task);
        log.info("资料导入请求已登记: bookId={}, title={}, sourceType={}, taskId={}",
                book.getId(),
                book.getTitle(),
                book.getSourceType(),
                task.getId());

        BookImportResponse response = new BookImportResponse();
        response.setBookId(book.getId());
        response.setTaskId(task.getId());
        response.setParseStatus(book.getParseStatus());
        response.setTaskStatus(task.getStatus());
        return response;
    }
}
