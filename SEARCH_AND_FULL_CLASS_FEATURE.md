# Tính năng Search Header & Lớp Full Slot

> Tài liệu hướng dẫn triển khai 2 tính năng:
> 1. **Search Header** - Tìm kiếm lớp học, giáo viên, môn học từ thanh header
> 2. **Full Slot Class** - Lớp đầy slot bị làm mờ, không đăng ký được và đẩy xuống cuối danh sách

---

## Mục lục

- [1. Search Header](#1-search-header)
  - [1.1. Backend (Java Spring Boot)](#11-backend-java-spring-boot)
  - [1.2. Frontend (React)](#12-frontend-react)
- [2. Full Slot Class](#2-full-slot-class)
  - [2.1. Backend](#21-backend)
  - [2.2. Frontend](#22-frontend)

---

# 1. Search Header

## 1.1. Backend (Java Spring Boot)

### SearchController.java

**Path:** `src/main/java/fpt/capstone/edu360managementsystem/controller/SearchController.java`

```java
package fpt.capstone.edu360managementsystem.controller;

import fpt.capstone.edu360managementsystem.dto.response.*;
import fpt.capstone.edu360managementsystem.service.ClassService;
import fpt.capstone.edu360managementsystem.service.SubjectService;
import fpt.capstone.edu360managementsystem.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for global search functionality.
 * Provides endpoints for searching classes, teachers, and subjects.
 *
 * @author 360edu
 * @version 1.0
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class SearchController {

    private final ClassService classService;
    private final TeacherService teacherService;
    private final SubjectService subjectService;

    /**
     * Performs a global search across multiple data types.
     *
     * @param q     the search keyword
     * @param limit maximum results per type (default 5)
     * @return search results from classes, teachers, and subjects
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> globalSearch(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit
    ) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "query", "",
                "classes", List.of(),
                "teachers", List.of(),
                "subjects", List.of(),
                "totalResults", 0
            ));
        }

        String searchTerm = q.trim();
        Map<String, Object> results = new HashMap<>();
        results.put("query", searchTerm);

        // Search classes - only PUBLIC classes, include currentStudents and maxStudents
        Page<ClassResponse> classResults = classService.getClassesWithPagination(
                searchTerm, "PUBLIC", null, null, null, null, 0, limit, "id", "desc"
        );
        results.put("classes", classResults.getContent());

        // Search teachers
        Page<TeacherResponse> teacherResults = teacherService.getTeachersWithPagination(
                searchTerm, null, 0, limit, "id", "desc"
        );
        results.put("teachers", teacherResults.getContent());

        // Search subjects
        Page<SubjectResponse> subjectResults = subjectService.getSubjectsWithPagination(
                searchTerm, "ACTIVE", 0, limit, "id", "desc"
        );
        results.put("subjects", subjectResults.getContent());

        int totalResults = classResults.getContent().size() 
                + teacherResults.getContent().size() 
                + subjectResults.getContent().size();
        results.put("totalResults", totalResults);

        return ResponseEntity.ok(results);
    }

    /**
     * Searches for classes by keyword.
     */
    @GetMapping("/classes")
    public ResponseEntity<Page<ClassResponse>> searchClasses(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ClassResponse> results = classService.getClassesWithPagination(
                q, "PUBLIC", null, null, null, null, page, size, "id", "desc"
        );
        return ResponseEntity.ok(results);
    }

    /**
     * Searches for teachers by keyword.
     */
    @GetMapping("/teachers")
    public ResponseEntity<Page<TeacherResponse>> searchTeachers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<TeacherResponse> results = teacherService.getTeachersWithPagination(
                q, null, page, size, "id", "desc"
        );
        return ResponseEntity.ok(results);
    }

    /**
     * Searches for subjects by keyword.
     */
    @GetMapping("/subjects")
    public ResponseEntity<Page<SubjectResponse>> searchSubjects(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<SubjectResponse> results = subjectService.getSubjectsWithPagination(
                q, "ACTIVE", page, size, "id", "desc"
        );
        return ResponseEntity.ok(results);
    }
}
```

### API Response Format

**Endpoint:** `GET /api/search?q=keyword&limit=5`

```json
{
  "query": "toán",
  "classes": [
    {
      "id": 1,
      "name": "Lớp Toán 10A",
      "teacherName": "Nguyễn Văn A",
      "subjectName": "Toán học",
      "currentStudents": 25,
      "maxStudents": 30,
      "price": 2000000
    }
  ],
  "teachers": [
    {
      "id": 1,
      "userId": 5,
      "fullName": "Nguyễn Văn A",
      "avatarUrl": "/uploads/avatars/teacher1.jpg",
      "specialization": "Toán học"
    }
  ],
  "subjects": [
    {
      "id": 1,
      "name": "Toán học",
      "code": "MATH"
    }
  ],
  "totalResults": 3
}
```

---

## 1.2. Frontend (React)

### A. Hook useDebounce

**Path:** `src/hooks/useDebounce.js`

```javascript
import { useEffect, useState } from "react";

/**
 * Trả về giá trị sau khi "im lặng" trong delay ms.
 * Dùng: const debouncedQ = useDebounce(q, 350)
 */
export default function useDebounce(value, delay = 300) {
  const [debounced, setDebounced] = useState(value);

  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);

  return debounced;
}
```

---

### B. Search API Service

**Path:** `src/services/search/search.api.js`

```javascript
import { http } from "../http";

/**
 * API Service cho tính năng tìm kiếm tổng hợp
 */
export const searchApi = {
  /**
   * Tìm kiếm tổng hợp - trả về kết quả từ nhiều nguồn
   * @param {string} query - Từ khóa tìm kiếm
   * @param {number} limit - Số lượng kết quả tối đa mỗi loại (mặc định 5)
   * @returns {Promise<{query: string, classes: Array, teachers: Array, subjects: Array, totalResults: number}>}
   */
  globalSearch: (query, limit = 5) =>
    http.get("/search", { params: { q: query, limit } }).then((r) => r.data),

  /**
   * Tìm kiếm lớp học
   */
  searchClasses: (query, page = 0, size = 10) =>
    http.get("/search/classes", { params: { q: query, page, size } }).then((r) => r.data),

  /**
   * Tìm kiếm giáo viên
   */
  searchTeachers: (query, page = 0, size = 10) =>
    http.get("/search/teachers", { params: { q: query, page, size } }).then((r) => r.data),

  /**
   * Tìm kiếm môn học
   */
  searchSubjects: (query, page = 0, size = 10) =>
    http.get("/search/subjects", { params: { q: query, page, size } }).then((r) => r.data),
};
```

---

### C. Header Component với Search

**Path:** `src/components/common/Header.jsx`

```jsx
/**
 * HEADER COMPONENT - Thanh điều hướng chính của website
 *
 * Chức năng Search:
 * - Tìm kiếm lớp học, giáo viên, môn học
 * - Debounce 300ms để giảm số lượng API calls
 * - Dropdown hiển thị kết quả tìm kiếm
 * - Hiển thị badge "ĐÃ ĐẦY" cho lớp full slot
 */

import { useState, useContext, useEffect, useRef } from "react";
import {
  Menu,
  X,
  User,
  GraduationCap,
  Search,
  LogOut,
  Calendar,
  BookOpen,
  Users,
  Loader2,
} from "lucide-react";
import { Button } from "../ui/Button";
import { Input } from "../ui/Input";
import { ImageWithFallback } from "../ui/ImageWithFallback.jsx";
import Logo from "./Logo";
import NotificationBell from "./NotificationBell";
import AuthContext from "../../context/AuthContext";
import { useToast } from "../../hooks/use-toast";
import { searchApi } from "../../services/search/search.api";
import useDebounce from "../../hooks/useDebounce";

export default function Header({ onNavigate, currentPage }) {
  const { user, logout } = useContext(AuthContext);
  const { success, error: showError } = useToast();
  
  // Helper để normalize role
  const hasRole = (roleToCheck) => {
    if (!user?.roles) return false;
    return user.roles.some(r => {
      const normalized = String(r).replace(/^ROLE_/, '').toLowerCase();
      return normalized === roleToCheck.toLowerCase();
    });
  };
  
  // ========== SEARCH STATES ==========
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState(null);
  const [isSearching, setIsSearching] = useState(false);
  const [showSearchDropdown, setShowSearchDropdown] = useState(false);
  const searchRef = useRef(null);
  
  // Debounce search query - chờ 300ms sau khi user ngừng gõ
  const debouncedSearchQuery = useDebounce(searchQuery, 300);

  // ========== SEARCH EFFECTS ==========
  
  // Thực hiện search khi debounced query thay đổi
  useEffect(() => {
    const performSearch = async () => {
      // Không search nếu query quá ngắn
      if (!debouncedSearchQuery || debouncedSearchQuery.trim().length < 2) {
        setSearchResults(null);
        setShowSearchDropdown(false);
        return;
      }

      setIsSearching(true);
      try {
        const results = await searchApi.globalSearch(debouncedSearchQuery, 5);
        setSearchResults(results);
        setShowSearchDropdown(true);
      } catch (error) {
        console.error("Search error:", error);
        setSearchResults(null);
      } finally {
        setIsSearching(false);
      }
    };

    performSearch();
  }, [debouncedSearchQuery]);

  // Đóng dropdown khi click ra ngoài
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (searchRef.current && !searchRef.current.contains(event.target)) {
        setShowSearchDropdown(false);
      }
    };

    if (showSearchDropdown) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [showSearchDropdown]);

  // ========== SEARCH HANDLERS ==========
  
  // Xử lý khi click vào kết quả tìm kiếm
  const handleSearchResultClick = (type, item) => {
    setShowSearchDropdown(false);
    setSearchQuery("");
    
    if (type === "class") {
      onNavigate({ type: "class", id: item.id });
    } else if (type === "teacher") {
      onNavigate({ type: "teacher", id: item.userId });
    } else if (type === "subject") {
      // Navigate đến trang classes với filter theo môn học
      onNavigate({ type: "classes", search: item.name });
    }
  };

  // ========== RENDER ==========
  return (
    <header className="sticky top-0 z-50 shadow-lg header-gradient">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          
          {/* LOGO */}
          <button
            onClick={() => onNavigate({ type: "home" })}
            className="flex items-center gap-3 hover:opacity-90 transition-opacity"
          >
            <div className="w-12 h-12 bg-white rounded-xl flex items-center justify-center shadow-lg p-2">
              <Logo />
            </div>
            <div>
              <h1 className="text-white text-xl font-bold">360edu</h1>
            </div>
          </button>

          {/* ========== THANH TÌM KIẾM ========== */}
          <div className="hidden lg:block flex-1 max-w-md mx-4" ref={searchRef}>
            <div className="relative">
              {/* Icon loading hoặc search */}
              {isSearching ? (
                <Loader2 className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/70 animate-spin" />
              ) : (
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/70" />
              )}
              
              {/* Input tìm kiếm */}
              <Input
                type="text"
                placeholder="Tìm kiếm lớp học, giáo viên, môn học..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => searchResults && setShowSearchDropdown(true)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && searchQuery.trim()) {
                    e.preventDefault();
                    setShowSearchDropdown(false);
                    onNavigate({ type: "classes", search: searchQuery.trim() });
                    setSearchQuery("");
                  }
                }}
                className="pl-10 pr-4 h-10 bg-white/10 backdrop-blur-sm border-white/20 text-white placeholder:text-white/70 focus:bg-white/20 focus:border-white/30 rounded-lg"
              />
              
              {/* ========== SEARCH RESULTS DROPDOWN ========== */}
              {showSearchDropdown && searchResults && (
                <div className="absolute top-full left-0 right-0 mt-2 bg-white rounded-lg shadow-2xl border border-gray-200 overflow-hidden z-50 max-h-96 overflow-y-auto">
                  
                  {/* Không có kết quả */}
                  {searchResults.totalResults === 0 ? (
                    <div className="p-4 text-center text-gray-500">
                      Không tìm thấy kết quả cho "{searchResults.query}"
                    </div>
                  ) : (
                    <>
                      {/* ===== KẾT QUẢ LỚP HỌC ===== */}
                      {searchResults.classes?.length > 0 && (
                        <div>
                          <div className="px-4 py-2 bg-gray-50 border-b flex items-center gap-2">
                            <GraduationCap className="w-4 h-4 text-blue-600" />
                            <span className="text-sm font-medium text-gray-700">Lớp học</span>
                          </div>
                          {searchResults.classes.map((cls) => {
                            const currentStudents = cls.currentStudents || 0;
                            const maxStudents = cls.maxStudents || 30;
                            const isFull = currentStudents >= maxStudents;
                            
                            return (
                              <button
                                key={`class-${cls.id}`}
                                onClick={() => handleSearchResultClick("class", cls)}
                                className={`w-full px-4 py-3 text-left transition-colors flex items-center gap-3 border-b border-gray-100 last:border-b-0 relative ${
                                  isFull 
                                    ? "bg-gray-50 opacity-70 hover:opacity-90 hover:bg-gray-100" 
                                    : "hover:bg-blue-50"
                                }`}
                              >
                                {/* Icon */}
                                <div className={`w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 ${
                                  isFull ? "bg-gray-200" : "bg-blue-100"
                                }`}>
                                  <GraduationCap className={`w-5 h-5 ${isFull ? "text-gray-500" : "text-blue-600"}`} />
                                </div>
                                
                                {/* Thông tin */}
                                <div className="flex-1 min-w-0">
                                  <div className="flex items-center gap-2">
                                    <p className={`font-medium truncate ${isFull ? "text-gray-500" : "text-gray-900"}`}>
                                      {cls.name}
                                    </p>
                                    {/* Badge ĐÃ ĐẦY */}
                                    {isFull && (
                                      <span className="px-2 py-0.5 text-xs font-bold bg-red-100 text-red-600 rounded-full whitespace-nowrap">
                                        ĐÃ ĐẦY
                                      </span>
                                    )}
                                  </div>
                                  <p className="text-sm text-gray-500 truncate">
                                    {cls.teacherName} • {cls.subjectName}
                                    {!isFull && ` • ${currentStudents}/${maxStudents} học sinh`}
                                  </p>
                                </div>
                                
                                {/* Giá */}
                                {cls.price && (
                                  <span className={`text-sm font-medium ${isFull ? "text-gray-400" : "text-green-600"}`}>
                                    {new Intl.NumberFormat("vi-VN").format(cls.price)}đ
                                  </span>
                                )}
                              </button>
                            );
                          })}
                        </div>
                      )}

                      {/* ===== KẾT QUẢ GIÁO VIÊN ===== */}
                      {searchResults.teachers?.length > 0 && (
                        <div>
                          <div className="px-4 py-2 bg-gray-50 border-b flex items-center gap-2">
                            <Users className="w-4 h-4 text-purple-600" />
                            <span className="text-sm font-medium text-gray-700">Giáo viên</span>
                          </div>
                          {searchResults.teachers.map((teacher) => (
                            <button
                              key={`teacher-${teacher.id}`}
                              onClick={() => handleSearchResultClick("teacher", teacher)}
                              className="w-full px-4 py-3 text-left hover:bg-purple-50 transition-colors flex items-center gap-3 border-b border-gray-100 last:border-b-0"
                            >
                              <ImageWithFallback
                                src={teacher.avatarUrl}
                                alt={teacher.fullName}
                                className="w-10 h-10 rounded-full object-cover flex-shrink-0"
                                fallbackType="avatar"
                              />
                              <div className="flex-1 min-w-0">
                                <p className="font-medium text-gray-900 truncate">{teacher.fullName}</p>
                                <p className="text-sm text-gray-500 truncate">
                                  {teacher.specialization || "Giáo viên"}
                                </p>
                              </div>
                            </button>
                          ))}
                        </div>
                      )}

                      {/* ===== KẾT QUẢ MÔN HỌC ===== */}
                      {searchResults.subjects?.length > 0 && (
                        <div>
                          <div className="px-4 py-2 bg-gray-50 border-b flex items-center gap-2">
                            <BookOpen className="w-4 h-4 text-green-600" />
                            <span className="text-sm font-medium text-gray-700">Môn học</span>
                          </div>
                          {searchResults.subjects.map((subject) => (
                            <button
                              key={`subject-${subject.id}`}
                              onClick={() => handleSearchResultClick("subject", subject)}
                              className="w-full px-4 py-3 text-left hover:bg-green-50 transition-colors flex items-center gap-3 border-b border-gray-100 last:border-b-0"
                            >
                              <div className="w-10 h-10 rounded-lg bg-green-100 flex items-center justify-center flex-shrink-0">
                                <BookOpen className="w-5 h-5 text-green-600" />
                              </div>
                              <div className="flex-1 min-w-0">
                                <p className="font-medium text-gray-900 truncate">{subject.name}</p>
                              </div>
                            </button>
                          ))}
                        </div>
                      )}
                    </>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Phần còn lại của Header (Navigation, Profile, etc.) */}
          {/* ... */}
          
        </div>
      </div>
    </header>
  );
}
```

---

# 2. Full Slot Class

## 2.1. Backend

Không cần thêm code Backend mới. ClassResponse đã có sẵn các field:
- `currentStudents` - Số học sinh hiện tại
- `maxStudents` - Số học sinh tối đa

Frontend sẽ tự so sánh để xác định lớp đã đầy hay chưa.

---

## 2.2. Frontend

### ClassList.jsx - Danh sách lớp học

**Path:** `src/pages/guest/classes/ClassList.jsx`

#### A. Logic Sort - Đẩy lớp full xuống cuối

```javascript
// Trong hàm fetchClasses(), sau khi lấy data từ API:

// 5. Sort: Push full classes to the end
content.sort((a, b) => {
  const aFull = (a.currentStudents || 0) >= (a.maxStudents || 30);
  const bFull = (b.currentStudents || 0) >= (b.maxStudents || 30);
  
  if (aFull && !bFull) return 1;   // a full, b không -> a đi sau
  if (!aFull && bFull) return -1;  // a không full, b full -> a đi trước
  return 0;                         // Giữ nguyên thứ tự
});

setClasses(content);
```

---

#### B. Render Card với styling cho lớp full

```jsx
{classes.map((c, idx) => {
  // Tính toán trạng thái full
  const currentStudents = c.currentStudents || 0;
  const maxStudents = c.maxStudents || 30;
  const enrollmentPercentage = (currentStudents / maxStudents) * 100;
  const isFull = currentStudents >= maxStudents;

  return (
    <Card
      key={c.id}
      className={`group overflow-hidden transition-all duration-300 cursor-pointer border-2 flex flex-col h-full relative ${
        isFull
          ? "opacity-60 grayscale-[30%] border-gray-300 hover:opacity-80 hover:grayscale-0"
          : "border-transparent hover:border-blue-200 hover:shadow-2xl hover:-translate-y-2"
      }`}
      onClick={() => goDetail(c.id)}
    >
      {/* ===== BADGE "ĐÃ ĐẦY" OVERLAY ===== */}
      {isFull && (
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-20 pointer-events-none">
          <div className="bg-red-600 text-white px-6 py-3 rounded-lg shadow-2xl transform -rotate-12 border-4 border-white">
            <span className="font-bold text-lg tracking-wider">ĐÃ ĐẦY</span>
          </div>
        </div>
      )}

      {/* ===== CARD HEADER VỚI GRADIENT ===== */}
      <div className={`bg-gradient-to-br ${gradients[idx % gradients.length]} h-44 relative`}>
        <div className={`absolute inset-0 ${
          isFull ? "bg-white/30" : "bg-black/10 group-hover:bg-black/20"
        } transition-all`}></div>
        
        {/* Badges */}
        <div className="absolute top-4 left-4 right-4 flex items-start justify-between">
          <div className="flex flex-col gap-2">
            <Badge className="bg-white/95 text-gray-900 backdrop-blur-sm shadow-lg w-fit font-medium">
              {c.subjectName || "Môn học"}
            </Badge>
            <Badge className={c.online 
              ? "bg-green-500/90 backdrop-blur-sm shadow-lg w-fit"
              : "bg-blue-500/90 backdrop-blur-sm shadow-lg w-fit"
            }>
              {c.online ? "Online" : "Offline"}
            </Badge>
          </div>
          <div className="bg-white/20 backdrop-blur-md rounded-full p-2.5 shadow-lg">
            <BookOpen className="w-6 h-6 text-white" />
          </div>
        </div>
      </div>

      {/* ===== CARD CONTENT ===== */}
      <CardContent className="p-5 relative flex-1 flex flex-col">
        {/* Teacher Avatar */}
        <div className="absolute -top-10 right-4">
          <div className="w-20 h-20 rounded-full bg-white ring-4 ring-white shadow-xl flex items-center justify-center overflow-hidden">
            {c.teacherAvatarUrl ? (
              <img src={c.teacherAvatarUrl} alt={c.teacherFullName} className="w-full h-full object-cover" />
            ) : (
              <div className="w-full h-full rounded-full bg-gradient-to-br from-blue-100 to-purple-100 flex items-center justify-center">
                <span className="text-2xl font-bold text-blue-600">
                  {(c.teacherFullName || "G").charAt(0).toUpperCase()}
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Class Name */}
        <h3 className="text-xl font-bold text-gray-900 mb-2 line-clamp-2 group-hover:text-blue-600 transition-colors pr-24">
          {c.name || `Lớp ${c.subjectName || "học"}`}
        </h3>

        {/* Teacher Info */}
        <div className="flex items-center gap-2 mb-3">
          <Users className="w-4 h-4 text-blue-600" />
          <span className="text-sm font-medium text-gray-700">
            {c.teacherFullName || "Đang cập nhật"}
          </span>
        </div>

        {/* Schedule Info */}
        <div className="space-y-2 mb-4">
          {Array.isArray(c.schedule) && c.schedule.length > 0 ? (
            <div className="text-sm text-gray-600">
              {dayLabelVi(c.schedule[0].dayOfWeek)} • {c.schedule[0].startTime?.slice(0, 5)} - {c.schedule[0].endTime?.slice(0, 5)}
            </div>
          ) : (
            <div className="text-sm text-gray-600">Thứ Hai, Tư, Sáu • 19:00 - 21:00</div>
          )}
        </div>

        {/* Spacer */}
        <div className="flex-1"></div>

        {/* ===== ENROLLMENT PROGRESS ===== */}
        <div className={`mb-4 rounded-lg p-3 ${isFull ? 'bg-red-50 ring-2 ring-red-200' : 'bg-gray-50'}`}>
          <div className="flex items-center justify-between text-xs mb-2">
            <span className={`font-medium ${isFull ? 'text-red-600' : 'text-gray-600'}`}>
              {isFull ? '🚫 Lớp đã đầy' : 'Đã đăng ký'}
            </span>
            <span className={`font-bold ${isFull ? 'text-red-600' : 'text-blue-600'}`}>
              {currentStudents}/{maxStudents}
            </span>
          </div>
          <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full transition-all duration-500 ${
                isFull 
                  ? 'bg-gradient-to-r from-red-500 to-red-600' 
                  : 'bg-gradient-to-r from-blue-500 to-purple-500'
              }`}
              style={{ width: `${enrollmentPercentage}%` }}
            />
          </div>
        </div>

        {/* ===== CTA BUTTON ===== */}
        <Button 
          className={`w-full shadow-lg group-hover:shadow-xl transition-all ${
            isFull
              ? 'bg-gray-400 hover:bg-gray-500 text-white'
              : 'bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white'
          }`}
        >
          <span className="font-medium">
            {isFull ? 'Xem chi tiết' : 'Xem chi tiết lớp học'}
          </span>
        </Button>
      </CardContent>
    </Card>
  );
})}
```

---

## Tổng hợp CSS Classes cho lớp Full

| Tính năng | CSS Classes |
|-----------|-------------|
| **Làm mờ card** | `opacity-60 grayscale-[30%]` |
| **Hover khôi phục** | `hover:opacity-80 hover:grayscale-0` |
| **Border xám** | `border-gray-300` |
| **Badge "ĐÃ ĐẦY"** | `bg-red-600 text-white transform -rotate-12 border-4 border-white` |
| **Progress container đỏ** | `bg-red-50 ring-2 ring-red-200` |
| **Progress bar đỏ** | `bg-gradient-to-r from-red-500 to-red-600` |
| **Text đỏ** | `text-red-600` |
| **Button disabled** | `bg-gray-400 hover:bg-gray-500` |

---

## Sơ đồ luồng hoạt động

### Search Flow:
```
User gõ keyword → useState → useDebounce (300ms) → API call → setSearchResults → Render dropdown
```

### Full Class Flow:
```
API trả về classes[] → Sort (full → cuối) → Render với conditional styling
                           ↓
              isFull = currentStudents >= maxStudents
                           ↓
              - Thêm class mờ + grayscale
              - Hiển thị badge "ĐÃ ĐẦY"
              - Progress bar đỏ
              - Button disabled style
```

---

## Lưu ý khi triển khai

1. **Backend Response** phải đảm bảo trả về `currentStudents` và `maxStudents` trong ClassResponse
2. **Debounce** giúp giảm số lượng API calls khi user đang gõ
3. **Sort client-side** được thực hiện sau khi nhận data từ API
4. **CSS Tailwind** cần đảm bảo đã config đúng trong `tailwind.config.js`

---

*Tài liệu được tạo tự động bởi GitHub Copilot - 360edu Project*
