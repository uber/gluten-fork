#pragma once

#include <iostream>
#include <string>
#include <vector>

namespace gluten {

// the functions defined in this file are used to debug
// the token `ToString` means the method of `ToString()`
// the token `2String` means the method of `toString()`

#ifdef GLUTEN_PRINT_DEBUG

template <typename T>
static inline void Print(const T& t) {
  std::cout << t;
}

template <typename T>
static inline void PrintLF(const T& t) {
  std::cout << t << std::endl;
}

template <typename A, typename B>
static inline void Print(const A& a, const B& b) {
  std::cout << a << b;
}

template <typename A, typename B>
static inline void PrintLF(const A& a, const B& b) {
  std::cout << a << b << std::endl;
}

template <typename A, typename B>
static inline void PrintSplit(const A& a, const B& b, const std::string split = ": ") {
  std::cout << a << split << b;
}

template <typename A, typename B>
static inline void PrintSplitLF(const A& a, const B& b, const std::string split = ": ") {
  std::cout << a << split << b << std::endl;
}

template <typename A, typename B>
static inline void PrintEQ(const A& a, const B& b) {
  std::cout << a << " = " << b;
}

template <typename A, typename B>
static inline void PrintEQLF(const A& a, const B& b) {
  std::cout << a << " = " << b << std::endl;
}

template <typename A, typename B>
static inline void PrintVS(const A& a, const B& b) {
  std::cout << a << " vs " << b;
}

template <typename A, typename B>
static inline void PrintVSLF(const A& a, const B& b) {
  std::cout << a << " vs " << b << std::endl;
}

template <typename E>
static inline void PrintElement(const E& e, bool first = false) {
  if (!first) {
    std::cout << ", ";
  }
  std::cout << e;
}

template <typename ITERATOR>
static inline void PrintRange(ITERATOR begin, ITERATOR end) {
  std::cout << "{ ";
  for (; begin != end; ++begin) {
    std::cout << *begin << " ";
  }
  std::cout << "}" << std::endl;
}

template <typename C>
static inline void PrintContainer(const C& c, const std::string& containerName = "") {
  if (!containerName.empty()) {
    std::cout << containerName << " ";
  }
  std::cout << "size = " << c.size() << " ";
  PrintRange(c.begin(), c.end());
}

template <typename A, typename B>
static inline void PrintAB2String(const A& a, const B& b) {
  std::cout << a << " = " << b.toString() << std::endl;
}

template <typename T>
static inline void Print2String(const T& t, const std::string& prefix = "") {
  if (!prefix.empty()) {
    std::cout << prefix << ": ";
  }
  std::cout << t.toString() << std::endl;
}

template <typename ITERATOR>
static inline void PrintRangeToString(ITERATOR begin, ITERATOR end) {
  std::cout << "{ ";
  for (; begin != end; ++begin) {
    std::cout << begin->ToString() << " ";
    ++begin;
  }
  std::cout << "}";
}

template <typename ITERATOR>
static inline void PrintRange2String(ITERATOR begin, ITERATOR end) {
  std::cout << "{ ";
  for (; begin != end; ++begin) {
    std::cout << begin->toString() << " ";
    ++begin;
  }
  std::cout << "}";
}

template <typename C>
static inline void PrintContainerToString(const C& c, const std::string& containerName = "") {
  if (!containerName.empty()) {
    std::cout << containerName << " ";
  }
  std::cout << "size = " << c.size() << std::endl;
  PrintRangeToString(c.begin(), c.end());
}

template <typename C>
static inline void PrintContainer2String(const C& c, const std::string& containerName = "") {
  if (!containerName.empty()) {
    std::cout << containerName << " ";
  }
  std::cout << "size = " << c.size() << std::endl;
  PrintRange2String(c.begin(), c.end());
}

template <typename C>
static inline void PrintVectorToString(const C& c, const std::string& containerName = "") {
  std::cout << containerName << " = {";
  for (auto& x : c) {
    std::cout << " " << x->ToString();
  }
  std::cout << " }" << std::endl;
}

template <typename C>
static inline void PrintVector2String(const C& c, const std::string& containerName = "") {
  std::cout << containerName << " = {";
  for (auto& x : c) {
    std::cout << " " << x->toString();
  }
  std::cout << " }" << std::endl;
}

template <typename V>
static inline void PrintVectorMapping(const V& v, const std::string& vectorName = "") {
  std::cout << vectorName << "\n{\n";
  for (size_t i = 0; i < v.size(); ++i) {
    Print("\t");
    PrintSplitLF(i, v[i], " -> ");
  }
  std::cout << "}" << std::endl;
}

template <typename V>
static inline void PrintVectorRange(const V& v, unsigned int begin, unsigned int end) {
  std::cout << "{";
  auto index = begin;
  for (; index != end; ++index) {
    PrintElement(v[index], index == begin);
  }

  std::cout << "}" << std::endl;
}

#define PRINT(a) PrintSplit(#a, a)
#define PRINTLF(a) PrintSplitLF(#a, a)

#define PRINT_FUNCTION_NAME() std::cout << __func__ << std::endl;
#define PRINT_FUNCTION_SPLIT_LINE() std::cout << "===== " << __func__ << " ======" << std::endl;

#define PRINT_CONTAINER(c) PrintContainer(c, #c)

#define PRINT_CONTAINER_TO_STRING(v) PrintContainerToString(v, #v)
#define PRINT_CONTAINER_2_STRING(v) PrintContainer2String(v, #v)

#define PRINT_VECTOR_TO_STRING(v) PrintVectorToString(v, #v)
#define PRINT_VECTOR_2_STRING(v) PrintVector2String(v, #v)

#define PRINT_VECTOR_MAPPING(v) PrintVectorMapping(v, #v)

#else // GLUTEN_PRINT_DEBUG

template <typename T>
static inline void Print(const T& t) {}

template <typename T>
static inline void PrintLF(const T& t) {}

template <typename A, typename B>
static inline void Print(const A& a, const B& b) {}

template <typename A, typename B>
static inline void PrintLF(const A& a, const B& b) {}

template <typename A, typename B>
static inline void PrintSplit(const A& a, const B& b, const std::string split = ": ") {}

template <typename A, typename B>
static inline void PrintSplitLF(const A& a, const B& b, const std::string split = ": ") {}

template <typename A, typename B>
static inline void PrintEQ(const A& a, const B& b) {}

template <typename A, typename B>
static inline void PrintEQLF(const A& a, const B& b) {}

template <typename A, typename B>
static inline void PrintVS(const A& a, const B& b) {}

template <typename A, typename B>
static inline void PrintVSLF(const A& a, const B& b) {}

template <typename E>
static inline void PrintElement(const E& e, bool first = false) {}

template <typename ITERATOR>
static inline void PrintRange(ITERATOR begin, ITERATOR end) {}

template <typename C>
static inline void PrintContainer(const C& c, const std::string& containerName = "") {}

template <typename A, typename B>
static inline void PrintAB2String(const A& a, const B& b) {}

template <typename T>
static inline void Print2String(const T& t, const std::string& prefix = "") {}

template <typename ITERATOR>
static inline void PrintRangeToString(ITERATOR begin, ITERATOR end) {}

template <typename C>
static inline void PrintContainerToString(const C& c, const std::string& containerName = "") {}

template <typename C>
static inline void PrintContainer2String(const C& c, const std::string& containerName = "") {}

template <typename C>
static inline void PrintVectorToString(const C& c, const std::string& containerName = "") {}

template <typename C>
static inline void PrintVector2String(const C& c, const std::string& containerName = "") {}

template <typename V>
static inline void PrintVectorMapping(const V& v, const std::string& vectorName = "") {}

template <typename V>
static inline void PrintVectorRange(const V& v, unsigned int from, unsigned int to) {}

#define PRINT(a)
#define PRINTLF(a)

#define PRINT_FUNCTION_NAME()
#define PRINT_FUNCTION_SPLIT_LINE()

#define PRINT_CONTAINER(c)

#define PRINT_CONTAINER_TO_STRING(c)
#define PRINT_CONTAINER_2_STRING(c)

#define PRINT_VECTOR_TO_STRING(v)
#define PRINT_VECTOR_2_STRING(v)

#define PRINT_VECTOR_MAPPING(v)

#endif // GLUTEN_PRINT_DEBUG

} // namespace gluten