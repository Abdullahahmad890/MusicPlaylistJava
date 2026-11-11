#include <iostream>
#include <string>
#include <sstream>
#include <vector>
using namespace std;

struct Node {
    int id;
    string title, artist;
    int duration;
    Node *next;
};

Node *first = NULL;
Node *current = NULL;
int total = 0;

// Decode Base64 text sent by the Java interface.
string decodeBase64(const string &input) {
    const string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    string output;
    int val = 0, valb = -8;
    for (unsigned char c : input) {
        if (c == '=') break;
        size_t pos = chars.find(c);
        if (pos == string::npos) continue;
        val = (val << 6) + (int)pos;
        valb += 6;
        if (valb >= 0) {
            output.push_back(char((val >> valb) & 0xFF));
            valb -= 8;
        }
    }
    return output;
}

// Encode Base64 text before sending it back to Java.
string encodeBase64(const string &input) {
    const string chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    string output;
    int val = 0, valb = -6;
    for (unsigned char c : input) {
        val = (val << 8) + c;
        valb += 8;
        while (valb >= 0) {
            output.push_back(chars[(val >> valb) & 0x3F]);
            valb -= 6;
        }
    }
    if (valb > -6) output.push_back(chars[((val << 8) >> (valb + 8)) & 0x3F]);
    while (output.size() % 4) output.push_back('=');
    return output;
}

Node* searchById(int id) {
    if (first == NULL) return NULL;
    Node *p = first;
    do {
        if (p->id == id) return p;
        p = p->next;
    } while (p != first);
    return NULL;
}

Node* searchByTitle(const string &title) {
    if (first == NULL) return NULL;
    Node *p = first;
    do {
        if (p->title == title) return p;
        p = p->next;
    } while (p != first);
    return NULL;
}

string songResponse(Node *p) {
    if (p == NULL) return "NOT_FOUND";
    return "SONG|" + to_string(p->id) + "|" + encodeBase64(p->title) + "|" +
           encodeBase64(p->artist) + "|" + to_string(p->duration);
}

int main() {
    string line;
    while (getline(cin, line)) {
        stringstream ss(line);
        string command, a, b, c;
        getline(ss, command, '|');
        getline(ss, a, '|');
        getline(ss, b, '|');
        getline(ss, c, '|');

        if (command == "ADD") {
            int id = stoi(a);
            string title = decodeBase64(b);
            string artist = decodeBase64(c);
            string d;
            getline(ss, d, '|');
            int duration = stoi(d);

            if (searchById(id) != NULL) {
                cout << "ERROR|A song with this ID already exists" << endl;
                continue;
            }

            Node *t = new Node;
            t->id = id;
            t->title = title;
            t->artist = artist;
            t->duration = duration;

            if (first == NULL) {
                first = t;
                t->next = first;
                current = first;
            } else {
                Node *last = first;
                while (last->next != first) last = last->next;
                last->next = t;
                t->next = first;
            }
            total++;
            cout << "OK|Song added successfully" << endl;
        }
        else if (command == "REMOVE_ID") {
            int id = stoi(a);
            if (first == NULL) {
                cout << "ERROR|Playlist is empty" << endl;
                continue;
            }
            Node *p = first, *prev = NULL;
            do {
                if (p->id == id) break;
                prev = p;
                p = p->next;
            } while (p != first);

            if (p->id != id) {
                cout << "ERROR|Song not found" << endl;
                continue;
            }
            if (p == first && p->next == first) {
                first = current = NULL;
            } else {
                if (prev == NULL) {
                    Node *last = first;
                    while (last->next != first) last = last->next;
                    first = first->next;
                    last->next = first;
                } else {
                    prev->next = p->next;
                }
                if (current == p) current = p->next;
            }
            delete p;
            total--;
            cout << "OK|Song removed successfully" << endl;
        }
        else if (command == "REMOVE_TITLE") {
            string title = decodeBase64(a);
            if (first == NULL) {
                cout << "ERROR|Playlist is empty" << endl;
                continue;
            }
            Node *p = first, *prev = NULL;
            do {
                if (p->title == title) break;
                prev = p;
                p = p->next;
            } while (p != first);

            if (p->title != title) {
                cout << "ERROR|Song not found" << endl;
                continue;
            }
            if (p == first && p->next == first) {
                first = current = NULL;
            } else {
                if (prev == NULL) {
                    Node *last = first;
                    while (last->next != first) last = last->next;
                    first = first->next;
                    last->next = first;
                } else {
                    prev->next = p->next;
                }
                if (current == p) current = p->next;
            }
            delete p;
            total--;
            cout << "OK|Song removed successfully" << endl;
        }
        else if (command == "SEARCH_ID") {
            cout << songResponse(searchById(stoi(a))) << endl;
        }
        else if (command == "SEARCH_TITLE") {
            cout << songResponse(searchByTitle(decodeBase64(a))) << endl;
        }
        else if (command == "NEXT") {
            if (current == NULL) cout << "NOT_FOUND" << endl;
            else {
                current = current->next;
                cout << songResponse(current) << endl;
            }
        }
        else if (command == "PREVIOUS") {
            if (current == NULL) cout << "NOT_FOUND" << endl;
            else {
                Node *p = current;
                while (p->next != current) p = p->next;
                current = p;
                cout << songResponse(current) << endl;
            }
        }
        else if (command == "CURRENT") {
            cout << songResponse(current) << endl;
        }
        else if (command == "COUNT") {
            cout << "COUNT|" << total << endl;
        }
        else if (command == "LIST") {
            cout << "LIST";
            if (first != NULL) {
                Node *p = first;
                do {
                    cout << "|" << p->id << "|" << encodeBase64(p->title)
                         << "|" << encodeBase64(p->artist) << "|" << p->duration;
                    p = p->next;
                } while (p != first);
            }
            cout << endl;
        }
        else if (command == "EXIT") {
            break;
        }
        else {
            cout << "ERROR|Unknown command" << endl;
        }
    }

    while (first != NULL) {
        if (first->next == first) {
            delete first;
            first = NULL;
        } else {
            Node *last = first;
            while (last->next != first) last = last->next;
            last->next = first->next;
            Node *temp = first;
            first = first->next;
            delete temp;
        }
    }
    return 0;
}
