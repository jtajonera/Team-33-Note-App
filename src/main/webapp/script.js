// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/** Fetches categories from the server and adds them to the DOM. */
function loadCategories() {
  fetch('/categories').then(response => response.json()).then((categories) => {
    const categoriesElement = document.getElementById('categories-container');
    categoriesElement.innerHTML = '';
    
    categories.forEach((category) => {
      categoriesElement.appendChild(createCategoryElement(category));
    })
  });
}

/** Fetches notes from the server and adds them to the DOM. */
function loadNotes() {
  fetch('/form-handler').then(response => response.json()).then((notes) => {
    const notesElement = document.getElementById('notes-container');
    notesElement.innerHTML = '';
    notes.forEach((note) => {
      notesElement.appendChild(createNoteElement(note));
    })
  });
}

/** Creates a checkbox element that represents a category */
function createCategoryElement(category) {
  const categoryElement = document.createElement('label');
  categoryElement.className = 'category';
  categoryElement.innerHTML = category;

  const inputElement = document.createElement('input');
  inputElement.setAttribute('type', 'checkbox');
  
  categoryElement.appendChild(inputElement);
  return categoryElement;
}

/** Creates an element that represents a note, including its delete button. */
function createNoteElement(note) {
  const noteElement = document.createElement('div');
  noteElement.className = 'note';
  noteElement.style.cssText = 
      'display:block; margin-bottom:10px;';

  const imageElement = document.createElement('img');
  imageElement.src = note.imageUrl;

  const message = document.createElement('p');
  message.innerText = note.message;

  const deleteButtonElement = document.createElement('button');
  deleteButtonElement.innerText = 'Delete';
  deleteButtonElement.addEventListener('click', () => {
    deleteNote(note);

    // Remove the note from the DOM.
    noteElement.remove();
  });

  noteElement.appendChild(imageElement);
  noteElement.appendChild(message);
  noteElement.appendChild(createUlElement(note.categories));
  noteElement.appendChild(deleteButtonElement);
  return noteElement;
}


/** Creates a <ul> element containing categories. */
function createUlElement(categories) {
  const ulElement = document.createElement('ul');
  for (j = 0; j < categories.length; j++){
    const liElement = document.createElement('li');
    liElement.innerText = categories[j];
    ulElement.appendChild(liElement);
  }
  ulElement.style.cssText = 'font-size:13px;';
  return ulElement;
}

/** Tells the server to delete the note. */
function deleteNote(note) {
  const params = new URLSearchParams();
  params.append('id', note.id);
  fetch('/delete-note', {method: 'POST', body: params});
}

function fetchBlobstoreUrl() {
  fetch('/blobstore-upload-url')
      .then((response) => {
        return response.text();
      })
      .then((imageUploadUrl) => {
        const imageForm = document.getElementById('my-form');
        imageForm.action = imageUploadUrl;
        imageForm.classList.remove('hidden');
      });
}


// TODO: Loads the output file as a download link in html
function loadOutputDoc(){}